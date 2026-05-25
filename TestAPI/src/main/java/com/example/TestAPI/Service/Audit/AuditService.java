package com.example.TestAPI.Service.Audit;

import com.example.TestAPI.Model.Enum.ActionType;
import com.example.TestAPI.Model.User;

public interface AuditService {

    void log(User user, ActionType action, String details);
}
