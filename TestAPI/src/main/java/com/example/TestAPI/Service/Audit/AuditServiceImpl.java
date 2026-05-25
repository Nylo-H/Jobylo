package com.example.TestAPI.Service.Audit;

import com.example.TestAPI.Model.ActionLog;
import com.example.TestAPI.Model.Enum.ActionType;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final ActionLogRepository actionLogRepository;

    @Override
    public void log(User user, ActionType action, String details) {
        ActionLog log = ActionLog.builder()
                .user(user)
                .action(action)
                .details(details)
                .timestamp(new Date())
                .build();
        actionLogRepository.save(log);
    }
}
