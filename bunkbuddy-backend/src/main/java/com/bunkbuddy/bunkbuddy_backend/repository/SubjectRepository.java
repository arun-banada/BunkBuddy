package com.bunkbuddy.bunkbuddy_backend.repository;

import com.bunkbuddy.bunkbuddy_backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByUserId(Long userId);
}
