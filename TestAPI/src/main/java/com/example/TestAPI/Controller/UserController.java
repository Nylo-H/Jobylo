package com.example.TestAPI.Controller;

import com.example.TestAPI.DTO.User.UserCreateRequest;
import com.example.TestAPI.DTO.User.UserUpdateRequest;
import com.example.TestAPI.DTO.User.UserResponse;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Service.User.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public UserResponse createUser(@RequestBody UserCreateRequest request) {
        return userService.createUser(User.builder()
                .nom(request.firstName())
                .prenom(request.lastName())
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
                .nom(request.firstName())
                .prenom(request.lastName())
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
}