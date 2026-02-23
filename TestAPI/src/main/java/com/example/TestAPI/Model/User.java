package com.example.TestAPI.Model;



import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.Enum.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(name = "users") // user est mot réservé Postgres, donc users
@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean verified;

    private String photoProfil;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<KycDocument> kycDocuments;

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<JobOffer> jobsCreated;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<JobOffer> jobsAssigned;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<Message> messagesSent;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<Message> messagesReceived;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ActionLog> actionLogs;
}
