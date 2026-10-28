package fu.de190530.user.repository;

import fu.de190530.user.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByUsername(String value);
    boolean existsByUsernameAndUserIdNot(String value, Long id);
}
