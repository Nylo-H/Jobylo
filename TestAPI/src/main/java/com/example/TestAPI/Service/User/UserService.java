package com.example.TestAPI.Service.User;

import com.example.TestAPI.DTO.User.UserResponse;
import com.example.TestAPI.Model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse createUser(User user);
    UserResponse getUser(UUID id);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(UUID id, User user);
    void deleteUser(UUID id);
}
