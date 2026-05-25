package com.example.TestAPI.Config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbMigration {

    private final DataSource dataSource;

    @PostConstruct
    public void migrate() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE action_logs DROP CONSTRAINT IF EXISTS action_logs_action_check");
            log.info("Contrainte action_logs_action_check supprimée (si existante)");
        } catch (Exception e) {
            log.warn("Impossible de supprimer la contrainte action_logs_action_check: {}", e.getMessage());
        }
    }
}
