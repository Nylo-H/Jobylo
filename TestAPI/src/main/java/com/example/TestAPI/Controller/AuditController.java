package com.example.TestAPI.Controller;

import com.example.TestAPI.Model.ActionLog;
import com.example.TestAPI.Model.Enum.Role;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final ActionLogRepository actionLogRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ActionLog>> getMyLogs(@AuthenticationPrincipal User currentUser) {
        List<ActionLog> logs = actionLogRepository.findByUserOrderByTimestampDesc(currentUser);
        return ResponseEntity.ok(logs);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ActionLog>> getAllLogs() {
        List<ActionLog> logs = actionLogRepository.findAllByOrderByTimestampDesc();
        return ResponseEntity.ok(logs);
    }
}
