package com.example.TestAPI.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobCategory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private JobCategory parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    @OrderBy("displayOrder asc")
    private List<JobCategory> subcategories = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private int displayOrder = 0;
}
