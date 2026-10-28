package fu.de190530.user.common;

import fu.de190530.user.entity.*;
import fu.de190530.user.dto.*;
import org.springframework.beans.BeanUtils;

public final class UserMapper {
    private UserMapper() {}
    public static UserDTO toDTO(User entity) {
        var dto = new UserDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setRole(entity.getRole().name());
        dto.setStatus(entity.getStatus().name());
        return dto;
    }
    public static void apply(UserDTO dto, User entity) {
        BeanUtils.copyProperties(dto, entity, "userId");
        entity.setRole(Role.valueOf(dto.getRole()));
        entity.setStatus(UserStatus.valueOf(dto.getStatus()));
    }
}
