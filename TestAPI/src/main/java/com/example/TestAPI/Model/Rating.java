package com.example.TestAPI.Model;

import com.example.TestAPI.Model.Enum.RatingTarget;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"rater_id", "job_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rating {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "rater_id", nullable = false)
    private User rater;

    @ManyToOne
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobOffer job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatingTarget targetType;

    @Column(nullable = false)
    private int score;

    @Column(length = 500)
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}
