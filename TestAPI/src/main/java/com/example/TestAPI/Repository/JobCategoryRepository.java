package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobCategoryRepository extends JpaRepository<JobCategory, UUID> {
    List<JobCategory> findByParentIsNullOrderByDisplayOrderAsc();
    List<JobCategory> findByParentIdOrderByDisplayOrderAsc(UUID parentId);
    boolean existsByName(String name);
}
