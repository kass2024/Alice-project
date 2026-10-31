package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    List<LoginSession> findTop100ByOrderByCreatedAtDesc();
    List<LoginSession> findByActiveTrue();
}
