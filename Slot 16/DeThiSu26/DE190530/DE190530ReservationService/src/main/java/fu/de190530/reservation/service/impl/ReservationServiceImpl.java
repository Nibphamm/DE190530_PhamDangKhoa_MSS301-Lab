package fu.de190530.reservation.service.impl;

import fu.de190530.reservation.common.*;
import fu.de190530.reservation.dto.*;
import fu.de190530.reservation.entity.*;
import fu.de190530.reservation.client.RoomClient;
import fu.de190530.reservation.repository.ReservationRepository;
import fu.de190530.reservation.service.ReservationService;
import feign.FeignException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository repository;
    private final RoomClient client;
    private final Checks checks;
    public ReservationServiceImpl(ReservationRepository repository, RoomClient client, Checks checks) {
        this.repository = repository;
        this.client = client;
        this.checks = checks;
    }
    private Reservation find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(404, 4, "Reservation is not found"));
    }
    private RoomDTO fetch(Long id) {
        try {
            var response = client.detail(id);
            if (response != null && response.status() == 1 && response.data() != null) return response.data();
            if (response != null && response.status() == 4) return null;
            throw new ApiException(500, 0, "Invalid response from RoomService");
        } catch (FeignException.NotFound e) { return null; }
    }
    private RoomDTO requireMaster(Long id) {
        var data = fetch(id);
        if (data == null) throw new ApiException(404, 4, "Room ID is not found");
        return data;
    }
    private long days(Date start, Date end) {
        // java.sql.Date does not support toInstant(); use epoch milliseconds instead.
        var first = java.time.Instant.ofEpochMilli(start.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
        var last = java.time.Instant.ofEpochMilli(end.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
        long days = ChronoUnit.DAYS.between(first, last);
        if (days < 1) throw new ApiException(406, 2, "checkOutDate must be after checkInDate");
        return days;
    }
    private void compute(ReservationDTO dto, RoomDTO master) {
        if (dto.getNumberOfGuests() > master.getCapacity())
            throw new ApiException(406, 2, "numberOfGuests must not exceed capacity");
        dto.setTotalAmount(master.getPricePerNight().multiply(BigDecimal.valueOf(days(dto.getCheckInDate(), dto.getCheckOutDate()))));
        checks.validate(dto, true);
    }
    @Override
    public ReservationDTO create(ReservationDTO dto) {
        checks.validate(dto, true);
        days(dto.getCheckInDate(), dto.getCheckOutDate());
        var master = requireMaster(dto.getRoomId());
        if (!"AVAILABLE".equals(master.getStatus()))
            throw new ApiException(400, 5, "Room is not AVAILABLE");
        dto.setStatus("CONFIRMED");
        compute(dto, master);
        var entity = new Reservation();
        ReservationMapper.apply(dto, entity);
        return ReservationMapper.toDTO(repository.saveAndFlush(entity));
    }
    @Override
    public ReservationDTO update(Long id, ReservationDTO patch) {
        var entity = find(id);
        checks.validate(patch, false);
        var dto = ReservationMapper.toDTO(entity);
        // The original roomId is immutable, as requested for these exercises.
        Checks.merge(patch, dto, "reservationId", "roomId", "totalAmount");
        checks.validate(dto, true);
        days(dto.getCheckInDate(), dto.getCheckOutDate());
        var master = requireMaster(entity.getRoomId());
        compute(dto, master);
        // Validate before mutating the managed entity so rejected patches cannot flush.
        ReservationMapper.apply(dto, entity);
        return ReservationMapper.toDTO(repository.saveAndFlush(entity));
    }
    private ReservationDetailDTO toDetail(Reservation entity) {
        var dto = new ReservationDetailDTO();
        BeanUtils.copyProperties(ReservationMapper.toDTO(entity), dto);
        dto.setRoom(fetch(entity.getRoomId()));
        return dto;
    }
    @Override @Transactional(readOnly = true)
    public ReservationDetailDTO detail(Long id) { return toDetail(find(id)); }
    @Override
    public void delete(Long id) {
        var entity = find(id);
        entity.setStatus(ReservationStatus.CANCELLED);
        repository.saveAndFlush(entity);
    }
    @Override @Transactional(readOnly = true)
    public PageDTO list(int page, int size, String guestName, String status) {
        var paging = Checks.page(page, size, "reservationId");
        var state = Checks.filter(status, ReservationStatus.class);
        Specification<Reservation> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (guestName != null && !guestName.isBlank()) {
                String literal = guestName.toLowerCase(java.util.Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.like(cb.lower(root.get("guestName")), "%" + literal + "%", '!'));
            }
            if (state != null) predicates.add(cb.equal(root.get("status"), state));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = repository.findAll(spec, paging);
        return PageDTO.of(result, result.getContent().stream().map(this::toDetail).toList());
    }
}
