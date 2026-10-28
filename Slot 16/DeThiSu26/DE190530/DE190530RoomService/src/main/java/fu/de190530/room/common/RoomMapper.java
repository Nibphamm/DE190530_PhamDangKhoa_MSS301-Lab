package fu.de190530.room.common;

import fu.de190530.room.entity.*;
import fu.de190530.room.dto.*;
import org.springframework.beans.BeanUtils;

public final class RoomMapper {
    private RoomMapper() {}
    public static RoomDTO toDTO(Room entity) {
        var dto = new RoomDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setRoomType(entity.getRoomType().name());
        dto.setStatus(entity.getStatus().name());
        return dto;
    }
    public static void apply(RoomDTO dto, Room entity) {
        BeanUtils.copyProperties(dto, entity, "roomId");
        entity.setRoomType(RoomType.valueOf(dto.getRoomType()));
        entity.setStatus(RoomStatus.valueOf(dto.getStatus()));
    }
}
