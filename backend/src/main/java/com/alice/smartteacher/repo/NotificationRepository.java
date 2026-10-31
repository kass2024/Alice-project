package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findTop100ByOrderByCreatedAtDesc();
    long countByUserIdAndReadFlagFalse(Long userId);
}
