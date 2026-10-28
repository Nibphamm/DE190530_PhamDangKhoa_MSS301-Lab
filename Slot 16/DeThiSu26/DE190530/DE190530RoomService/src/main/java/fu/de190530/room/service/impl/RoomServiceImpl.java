package fu.de190530.room.service.impl;

import fu.de190530.room.common.*;
import fu.de190530.room.dto.*;
import fu.de190530.room.entity.*;
import fu.de190530.room.repository.RoomRepository;
import fu.de190530.room.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {
    private final RoomRepository repository;
    private final Checks checks;
    public RoomServiceImpl(RoomRepository repository, Checks checks) {
        this.repository = repository;
        this.checks = checks;
    }
    private Room find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(404, 4, "Room is not found"));
    }
    @Override
    public RoomDTO create(RoomDTO dto) {
        checks.validate(dto, true);
        if (repository.existsByRoomNumber(dto.getRoomNumber())) throw new ApiException(226, 3, "RoomNumber is duplicated");
        dto.setStatus("AVAILABLE");
        var entity = new Room();
        RoomMapper.apply(dto, entity);
        return RoomMapper.toDTO(repository.saveAndFlush(entity));
    }
    @Override
    public RoomDTO update(Long id, RoomDTO patch) {
        var entity = find(id);
        checks.validate(patch, false);
        var dto = RoomMapper.toDTO(entity);
        Checks.merge(patch, dto, "roomId");
        checks.validate(dto, true);
        if (repository.existsByRoomNumberAndRoomIdNot(dto.getRoomNumber(), id)) throw new ApiException(226, 3, "RoomNumber is duplicated");
        RoomMapper.apply(dto, entity);
        return RoomMapper.toDTO(repository.saveAndFlush(entity));
    }
    @Override @Transactional(readOnly = true)
    public RoomDTO detail(Long id) { return RoomMapper.toDTO(find(id)); }
    @Override
    public void delete(Long id) {
        var entity = find(id);
        entity.setStatus(RoomStatus.MAINTENANCE);
        repository.saveAndFlush(entity);
    }
    @Override @Transactional(readOnly = true)
    public PageDTO list(int page, int size, String roomType, String status) {
        var paging = Checks.page(page, size, "roomId");
        var kind = Checks.filter(roomType, RoomType.class);
        var state = Checks.filter(status, RoomStatus.class);
        Specification<Room> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (kind != null) predicates.add(cb.equal(root.get("roomType"), kind));
            if (state != null) predicates.add(cb.equal(root.get("status"), state));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = repository.findAll(spec, paging);
        return PageDTO.of(result, result.getContent().stream().map(RoomMapper::toDTO).toList());
    }
}
