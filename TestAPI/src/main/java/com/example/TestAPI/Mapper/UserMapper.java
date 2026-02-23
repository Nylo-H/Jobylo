package com.example.TestAPI.Mapper;

import com.example.TestAPI.DTO.User.UserCreateRequest;
import com.example.TestAPI.DTO.User.UserResponse;
import com.example.TestAPI.DTO.User.UserUpdateRequest;
import com.example.TestAPI.Model.User;
import org.apache.catalina.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role", target = "role")
    UserResponse toDTO(User user);

    User toEntity(UserCreateRequest request);

    void updateEntityFromDto(UserUpdateRequest request, @MappingTarget User user);

    // Méthode custom pour MapStruct
    default List<String> map(Set<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream()
                .map(Role::getName) // on prend juste le nom du rôle
                .toList();
    }
}