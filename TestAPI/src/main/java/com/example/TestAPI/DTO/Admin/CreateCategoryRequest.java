package com.example.TestAPI.DTO.Admin;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank String name,
        String description,
        String icon,
        UUID parentId,
        int displayOrder
) {}
