package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<UserAccount> findByRoleOrderByFullNameAsc(Enums.Role role);
    List<UserAccount> findByRoleAndActiveTrueOrderByFullNameAsc(Enums.Role role);
    List<UserAccount> findAllByOrderByFullNameAsc();
}
