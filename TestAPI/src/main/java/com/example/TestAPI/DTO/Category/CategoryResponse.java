package com.example.TestAPI.DTO.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String icon,
        UUID parentId,
        int displayOrder
) {}
