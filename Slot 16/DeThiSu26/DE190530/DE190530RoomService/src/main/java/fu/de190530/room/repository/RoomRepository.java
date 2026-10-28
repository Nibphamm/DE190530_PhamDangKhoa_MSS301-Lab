package fu.de190530.room.repository;

import fu.de190530.room.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    boolean existsByRoomNumber(String value);
    boolean existsByRoomNumberAndRoomIdNot(String value, Long id);
}
