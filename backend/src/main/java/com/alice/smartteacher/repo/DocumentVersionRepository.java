package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(Long documentId, int versionNumber);
}
