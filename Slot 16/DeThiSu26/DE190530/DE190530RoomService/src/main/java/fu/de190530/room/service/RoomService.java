package fu.de190530.room.service;

import fu.de190530.room.dto.*;

public interface RoomService {
    RoomDTO create(RoomDTO dto);
    RoomDTO update(Long id, RoomDTO dto);
    RoomDTO detail(Long id);
    void delete(Long id);
    PageDTO list(int page, int size, String roomType, String status);
}
