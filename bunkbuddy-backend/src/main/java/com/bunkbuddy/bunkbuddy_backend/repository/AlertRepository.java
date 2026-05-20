package com.bunkbuddy.bunkbuddy_backend.repository;

import com.bunkbuddy.bunkbuddy_backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);
}
