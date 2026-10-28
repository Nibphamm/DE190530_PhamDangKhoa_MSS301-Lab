package fu.de190530.post.common;

import fu.de190530.post.entity.*;
import fu.de190530.post.dto.*;
import org.springframework.beans.BeanUtils;

public final class PostMapper {
    private PostMapper() {}
    public static PostDTO toDTO(Post entity) {
        var dto = new PostDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setStatus(entity.getStatus().name());
        return dto;
    }
    public static void apply(PostDTO dto, Post entity) {
        BeanUtils.copyProperties(dto, entity, "postId");
        entity.setStatus(PostStatus.valueOf(dto.getStatus()));
    }
}
