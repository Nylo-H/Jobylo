package com.example.TestAPI.Controller;

import com.example.TestAPI.DTO.Audit.ActionLogResponse;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final ActionLogRepository actionLogRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ActionLogResponse>> getMyLogs(@AuthenticationPrincipal User currentUser) {
        List<ActionLogResponse> logs = actionLogRepository.findByUserOrderByTimestampDesc(currentUser)
                .stream()
                .map(log -> new ActionLogResponse(
                        log.getId(),
                        log.getUser().getId(),
                        log.getUser().getUsername(),
                        log.getAction(),
                        log.getDetails(),
                        log.getTimestamp()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ActionLogResponse>> getAllLogs() {
        List<ActionLogResponse> logs = actionLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(log -> new ActionLogResponse(
                        log.getId(),
                        log.getUser().getId(),
                        log.getUser().getUsername(),
                        log.getAction(),
                        log.getDetails(),
                        log.getTimestamp()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }
}
