package fu.de190530.reservation.service;

import fu.de190530.reservation.dto.*;

public interface ReservationService {
    ReservationDTO create(ReservationDTO dto);
    ReservationDTO update(Long id, ReservationDTO dto);
    ReservationDetailDTO detail(Long id);
    void delete(Long id);
    PageDTO list(int page, int size, String guestName, String status);
}
