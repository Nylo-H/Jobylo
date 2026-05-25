package com.example.TestAPI.Model;


import com.example.TestAPI.Model.Enum.KycDocumentType;
import com.example.TestAPI.Model.Enum.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String fileUrl;

    @Enumerated(EnumType.STRING)
    private KycDocumentType documentType;

    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    @ManyToOne
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    private String rejectionReason;
}