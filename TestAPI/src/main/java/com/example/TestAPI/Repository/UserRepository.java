package com.example.TestAPI.Repository;



import com.example.TestAPI.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // Méthodes pratiques pour vérifier l’existence
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

