package fu.de190530.reservation.dto;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import fu.de190530.reservation.common.Create;

public class ReservationDTO {
    private Long reservationId;

    @NotBlank(groups = Create.class) @Size(max = 100)
    private String guestName;

    @NotBlank(groups = Create.class) @Email @Size(max = 100)
    private String guestEmail;

    @NotBlank(groups = Create.class) @Size(max = 20)
    private String guestPhone;

    @NotNull(groups = Create.class)
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC", lenient = OptBoolean.FALSE)
    private Date checkInDate;

    @NotNull(groups = Create.class)
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC", lenient = OptBoolean.FALSE)
    private Date checkOutDate;

    @NotNull(groups = Create.class) @Min(1) @Max(10)
    private Integer numberOfGuests;

    @DecimalMin("0") @Digits(integer = 16, fraction = 2)
    private BigDecimal totalAmount;

    @Pattern(regexp = "CONFIRMED|CHECKED_IN|CHECKED_OUT|CANCELLED")
    private String status;

    @NotNull(groups = Create.class) @Min(1)
    private Long roomId;

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }

    public Date getCheckInDate() { return checkInDate; }
    public void setCheckInDate(Date checkInDate) { this.checkInDate = checkInDate; }

    public Date getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(Date checkOutDate) { this.checkOutDate = checkOutDate; }

    public Integer getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

}
