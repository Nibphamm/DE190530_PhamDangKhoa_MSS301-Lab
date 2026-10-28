package fu.de190530.user.service;

import fu.de190530.user.dto.*;

public interface UserService {
    UserDTO create(UserDTO dto);
    UserDTO update(Long id, UserDTO dto);
    UserDTO detail(Long id);
    void delete(Long id);
    PageDTO list(int page, int size, String role, String status);
}
