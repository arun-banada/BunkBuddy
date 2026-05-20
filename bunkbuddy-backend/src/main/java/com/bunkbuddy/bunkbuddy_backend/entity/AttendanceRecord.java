package com.bunkbuddy.bunkbuddy_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
@Table(name = "attendance_records")
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnore
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @JsonIgnore
    private Schedule schedule;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String status;
}
