package com.example.TestAPI.DTO.Admin;

import com.example.TestAPI.Model.Enum.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role
) {}
