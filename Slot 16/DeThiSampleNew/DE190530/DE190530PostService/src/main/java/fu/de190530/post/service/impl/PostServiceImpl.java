package fu.de190530.post.service.impl;

import fu.de190530.post.common.*;
import fu.de190530.post.dto.*;
import fu.de190530.post.entity.*;
import fu.de190530.post.client.UserClient;
import fu.de190530.post.repository.PostRepository;
import fu.de190530.post.service.PostService;
import feign.FeignException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@Transactional
public class PostServiceImpl implements PostService {
    private final PostRepository repository;
    private final UserClient client;
    private final Checks checks;
    public PostServiceImpl(PostRepository repository, UserClient client, Checks checks) {
        this.repository = repository;
        this.client = client;
        this.checks = checks;
    }
    private Post find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(404, 4, "Post is not found"));
    }
    private UserDTO fetch(Long id) {
        try {
            var response = client.detail(id);
            if (response != null && response.status() == 1 && response.data() != null) return response.data();
            if (response != null && response.status() == 4) return null;
            throw new ApiException(500, 0, "Invalid response from UserService");
        } catch (FeignException.NotFound e) { return null; }
    }
    private UserDTO requireMaster(Long id) {
        var data = fetch(id);
        if (data == null) throw new ApiException(404, 4, "User ID is not found");
        return data;
    }
    private long days(Date start, Date end) {
        // java.sql.Date does not support toInstant(); use epoch milliseconds instead.
        var first = java.time.Instant.ofEpochMilli(start.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
        var last = java.time.Instant.ofEpochMilli(end.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
        long days = ChronoUnit.DAYS.between(first, last);
        if (days < 1) throw new ApiException(406, 2, "endDate must be after startDate");
        return days;
    }
    private void compute(PostDTO dto, UserDTO master) {
        if (dto.getNumberOfPosts() > master.getMaxDailyPosts())
            throw new ApiException(406, 2, "numberOfPosts must not exceed maxDailyPosts");
        dto.setTotalCost(master.getRatePerPost().multiply(BigDecimal.valueOf(days(dto.getStartDate(), dto.getEndDate()))));
        checks.validate(dto, true);
    }
    @Override
    public PostDTO create(PostDTO dto) {
        checks.validate(dto, true);
        days(dto.getStartDate(), dto.getEndDate());
        var master = requireMaster(dto.getUserId());
        if (!"ACTIVE".equals(master.getStatus()))
            throw new ApiException(400, 5, "User is not ACTIVE");
        dto.setStatus("SCHEDULED");
        compute(dto, master);
        var entity = new Post();
        PostMapper.apply(dto, entity);
        return PostMapper.toDTO(repository.saveAndFlush(entity));
    }
    @Override
    public PostDTO update(Long id, PostDTO patch) {
        var entity = find(id);
        checks.validate(patch, false);
        var dto = PostMapper.toDTO(entity);
        // The original userId is immutable, as requested for these exercises.
        Checks.merge(patch, dto, "postId", "userId", "totalCost");
        checks.validate(dto, true);
        days(dto.getStartDate(), dto.getEndDate());
        var master = requireMaster(entity.getUserId());
        compute(dto, master);
        // Validate before mutating the managed entity so rejected patches cannot flush.
        PostMapper.apply(dto, entity);
        return PostMapper.toDTO(repository.saveAndFlush(entity));
    }
    private PostDetailDTO toDetail(Post entity) {
        var dto = new PostDetailDTO();
        BeanUtils.copyProperties(PostMapper.toDTO(entity), dto);
        dto.setUser(fetch(entity.getUserId()));
        return dto;
    }
    @Override @Transactional(readOnly = true)
    public PostDetailDTO detail(Long id) { return toDetail(find(id)); }
    @Override
    public void delete(Long id) {
        var entity = find(id);
        entity.setStatus(PostStatus.CANCELLED);
        repository.saveAndFlush(entity);
    }
    @Override @Transactional(readOnly = true)
    public PageDTO list(int page, int size, String advertiserName, String status) {
        var paging = Checks.page(page, size, "postId");
        var state = Checks.filter(status, PostStatus.class);
        Specification<Post> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (advertiserName != null && !advertiserName.isBlank()) {
                String literal = advertiserName.toLowerCase(java.util.Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.like(cb.lower(root.get("advertiserName")), "%" + literal + "%", '!'));
            }
            if (state != null) predicates.add(cb.equal(root.get("status"), state));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = repository.findAll(spec, paging);
        return PageDTO.of(result, result.getContent().stream().map(this::toDetail).toList());
    }
}
