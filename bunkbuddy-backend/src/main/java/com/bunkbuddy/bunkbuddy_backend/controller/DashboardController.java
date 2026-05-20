package com.bunkbuddy.bunkbuddy_backend.controller;

import com.bunkbuddy.bunkbuddy_backend.entity.Subject;
import com.bunkbuddy.bunkbuddy_backend.entity.User;
import com.bunkbuddy.bunkbuddy_backend.repository.SubjectRepository;
import com.bunkbuddy.bunkbuddy_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public DashboardController(SubjectRepository subjectRepository, UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getDashboardStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        List<Subject> subjects = subjectRepository.findByUserId(user.getId());
        
        int totalClasses = 0;
        int totalAttended = 0;
        int riskySubjects = 0;

        for (Subject subject : subjects) {
            totalClasses += subject.getTotalClasses();
            totalAttended += subject.getAttendedClasses();
            
            if (subject.getTotalClasses() > 0) {
                 double percentage = ((double) subject.getAttendedClasses() / subject.getTotalClasses()) * 100;
                 if (percentage < 75.0) {
                     riskySubjects++;
                 }
            }
        }

        double overallPercentage = totalClasses == 0 ? 100.0 : ((double) totalAttended / totalClasses) * 100;

        Map<String, Object> response = new HashMap<>();
        response.put("overallPercentage", overallPercentage);
        response.put("totalClasses", totalClasses);
        response.put("totalAttended", totalAttended);
        response.put("riskySubjects", riskySubjects);
        response.put("username", user.getUsername());
        
        return ResponseEntity.ok(response);
    }
}
