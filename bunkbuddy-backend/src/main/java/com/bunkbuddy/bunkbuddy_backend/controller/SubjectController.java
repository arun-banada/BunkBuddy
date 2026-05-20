package com.bunkbuddy.bunkbuddy_backend.controller;

import com.bunkbuddy.bunkbuddy_backend.entity.Subject;
import com.bunkbuddy.bunkbuddy_backend.entity.User;
import com.bunkbuddy.bunkbuddy_backend.repository.SubjectRepository;
import com.bunkbuddy.bunkbuddy_backend.repository.UserRepository;
import com.bunkbuddy.bunkbuddy_backend.service.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final PredictionService predictionService;

    public SubjectController(SubjectRepository subjectRepository, UserRepository userRepository, PredictionService predictionService) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.predictionService = predictionService;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<?> getSubjects() {
        User user = getCurrentUser();
        List<Subject> subjects = subjectRepository.findByUserId(user.getId());
        
        var subjectsWithStats = subjects.stream().map(subject -> {
            return predictionService.calculateSubjectStats(subject);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(subjectsWithStats);
    }

    @PostMapping
    public ResponseEntity<?> addSubject(@RequestBody Subject subject) {
        User user = getCurrentUser();
        subject.setUser(user);
        subjectRepository.save(subject);
        return ResponseEntity.ok(subject);
    }
}
