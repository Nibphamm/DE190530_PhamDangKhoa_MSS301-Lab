package fu.de190530.user.service.impl;

import fu.de190530.user.common.*;
import fu.de190530.user.dto.*;
import fu.de190530.user.entity.*;
import fu.de190530.user.repository.UserRepository;
import fu.de190530.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final Checks checks;
    public UserServiceImpl(UserRepository repository, Checks checks) {
        this.repository = repository;
        this.checks = checks;
    }
    private User find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(404, 4, "User is not found"));
    }
    @Override
    public UserDTO create(UserDTO dto) {
        checks.validate(dto, true);
        if (repository.existsByUsername(dto.getUsername())) throw new ApiException(226, 3, "Username is duplicated");
        dto.setStatus("ACTIVE");
        var entity = new User();
        UserMapper.apply(dto, entity);
        return UserMapper.toDTO(repository.saveAndFlush(entity));
    }
    @Override
    public UserDTO update(Long id, UserDTO patch) {
        var entity = find(id);
        checks.validate(patch, false);
        var dto = UserMapper.toDTO(entity);
        Checks.merge(patch, dto, "userId");
        checks.validate(dto, true);
        if (repository.existsByUsernameAndUserIdNot(dto.getUsername(), id)) throw new ApiException(226, 3, "Username is duplicated");
        UserMapper.apply(dto, entity);
        return UserMapper.toDTO(repository.saveAndFlush(entity));
    }
    @Override @Transactional(readOnly = true)
    public UserDTO detail(Long id) { return UserMapper.toDTO(find(id)); }
    @Override
    public void delete(Long id) {
        var entity = find(id);
        entity.setStatus(UserStatus.DEACTIVATED);
        repository.saveAndFlush(entity);
    }
    @Override @Transactional(readOnly = true)
    public PageDTO list(int page, int size, String role, String status) {
        var paging = Checks.page(page, size, "userId");
        var kind = Checks.filter(role, Role.class);
        var state = Checks.filter(status, UserStatus.class);
        Specification<User> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (kind != null) predicates.add(cb.equal(root.get("role"), kind));
            if (state != null) predicates.add(cb.equal(root.get("status"), state));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = repository.findAll(spec, paging);
        return PageDTO.of(result, result.getContent().stream().map(UserMapper::toDTO).toList());
    }
}
