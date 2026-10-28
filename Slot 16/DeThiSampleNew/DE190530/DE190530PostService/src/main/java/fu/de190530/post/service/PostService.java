package fu.de190530.post.service;

import fu.de190530.post.dto.*;

public interface PostService {
    PostDTO create(PostDTO dto);
    PostDTO update(Long id, PostDTO dto);
    PostDetailDTO detail(Long id);
    void delete(Long id);
    PageDTO list(int page, int size, String advertiserName, String status);
}
