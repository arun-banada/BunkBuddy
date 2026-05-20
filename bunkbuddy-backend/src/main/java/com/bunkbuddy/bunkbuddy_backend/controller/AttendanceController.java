package com.bunkbuddy.bunkbuddy_backend.controller;

import com.bunkbuddy.bunkbuddy_backend.entity.AttendanceRecord;
import com.bunkbuddy.bunkbuddy_backend.entity.Subject;
import com.bunkbuddy.bunkbuddy_backend.repository.AttendanceRecordRepository;
import com.bunkbuddy.bunkbuddy_backend.repository.SubjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SubjectRepository subjectRepository;

    public AttendanceController(AttendanceRecordRepository attendanceRecordRepository, SubjectRepository subjectRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.subjectRepository = subjectRepository;
    }

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> request) {
        Long subjectId = Long.parseLong(request.get("subjectId").toString());
        String status = request.get("status").toString(); // "PRESENT" or "ABSENT"

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        AttendanceRecord record = new AttendanceRecord();
        record.setSubject(subject);
        record.setDate(LocalDate.now());
        record.setStatus(status);

        subject.setTotalClasses(subject.getTotalClasses() + 1);
        if ("PRESENT".equalsIgnoreCase(status)) {
            subject.setAttendedClasses(subject.getAttendedClasses() + 1);
        }

        subjectRepository.save(subject);
        attendanceRecordRepository.save(record);

        return ResponseEntity.ok(Map.of("message", "Attendance marked successfully", "subjectId", subject.getId()));
    }
}
