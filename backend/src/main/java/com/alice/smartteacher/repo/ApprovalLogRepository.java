package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.ApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {
    List<ApprovalLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
