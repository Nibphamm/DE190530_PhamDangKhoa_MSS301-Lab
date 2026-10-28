package fu.de190530.reservation.common;

import fu.de190530.reservation.entity.*;
import fu.de190530.reservation.dto.*;
import org.springframework.beans.BeanUtils;

public final class ReservationMapper {
    private ReservationMapper() {}
    public static ReservationDTO toDTO(Reservation entity) {
        var dto = new ReservationDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setStatus(entity.getStatus().name());
        return dto;
    }
    public static void apply(ReservationDTO dto, Reservation entity) {
        BeanUtils.copyProperties(dto, entity, "reservationId");
        entity.setStatus(ReservationStatus.valueOf(dto.getStatus()));
    }
}
