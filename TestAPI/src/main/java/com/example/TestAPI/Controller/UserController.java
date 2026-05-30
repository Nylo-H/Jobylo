package com.example.TestAPI.Controller;

import com.example.TestAPI.DTO.User.UserCreateRequest;
import com.example.TestAPI.DTO.User.UserResponse;
import com.example.TestAPI.DTO.User.UserStatsResponse;
import com.example.TestAPI.DTO.User.UserUpdateRequest;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Service.Auth.AuthService;
import com.example.TestAPI.Service.User.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping
    public UserResponse createUser(@RequestBody UserCreateRequest request) {
        return userService.createUser(User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .email(request.email())
                .photoProfil(request.photoProfile())
                .build());
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable UUID id, @RequestBody UserUpdateRequest request) {
        User userToUpdate = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .email(request.email())
                .photoProfil(request.photoProfile())
                .build();
        return userService.updateUser(id, userToUpdate);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }

    @GetMapping("/me/stats")
    @PreAuthorize("isAuthenticated()")
    public UserStatsResponse getMyStats(@AuthenticationPrincipal User currentUser) {
        return authService.getUserStats(currentUser);
    }
}