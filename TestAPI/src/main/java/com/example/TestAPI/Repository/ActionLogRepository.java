package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.ActionLog;
import com.example.TestAPI.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionLogRepository extends JpaRepository<ActionLog, UUID> {

    List<ActionLog> findByUserOrderByTimestampDesc(User user);

    List<ActionLog> findAllByOrderByTimestampDesc();
}
