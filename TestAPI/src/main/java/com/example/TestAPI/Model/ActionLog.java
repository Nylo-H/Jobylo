package com.example.TestAPI.Model;

import com.example.TestAPI.Model.Enum.ActionType;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "action_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActionLog {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType action;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
}
