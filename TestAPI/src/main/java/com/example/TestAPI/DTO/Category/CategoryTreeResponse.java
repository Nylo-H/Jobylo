package com.example.TestAPI.DTO.Category;

import java.util.List;
import java.util.UUID;

public record CategoryTreeResponse(
        UUID id,
        String name,
        String description,
        String icon,
        int displayOrder,
        List<CategoryTreeResponse> subcategories
) {}
