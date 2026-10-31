package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.AcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {
    List<AcademicRecord> findAllByOrderByClassNameAscStudentNameAsc();
}
