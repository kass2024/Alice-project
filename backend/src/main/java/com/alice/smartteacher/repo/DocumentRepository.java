package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.SchoolDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<SchoolDocument, Long> {
    List<SchoolDocument> findAllByOrderByUpdatedAtDesc();
    List<SchoolDocument> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
    List<SchoolDocument> findByStatusInOrderByUpdatedAtDesc(List<Enums.DocStatus> statuses);
    long countByStatus(Enums.DocStatus status);
}
