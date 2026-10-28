package fu.de190530.room.dto;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import fu.de190530.room.common.Create;

public class RoomDTO {
    private Long roomId;

    @NotBlank(groups = Create.class) @Size(min = 1, max = 20)
    private String roomNumber;

    @NotNull(groups = Create.class) @Pattern(regexp = "SINGLE|DOUBLE|SUITE|DELUXE")
    private String roomType;

    @NotNull(groups = Create.class) @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2)
    private BigDecimal pricePerNight;

    @NotNull(groups = Create.class) @Min(1) @Max(10)
    private Integer capacity;

    @NotNull(groups = Create.class) @Min(1)
    private Integer floor;

    @Pattern(regexp = "AVAILABLE|OCCUPIED|MAINTENANCE")
    private String status;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

}
