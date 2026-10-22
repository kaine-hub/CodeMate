package com.codemate.alpha.controller;

import com.codemate.alpha.entity.User;
import com.codemate.alpha.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // REGISTER USER
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {

        // Check mandatory fields
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("First name is required");
        }

        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Last name is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Password is required");
        }

        // At least ONE profile link is required
        if ((user.getGithubLink() == null || user.getGithubLink().trim().isEmpty())
                && (user.getLinkedinLink() == null || user.getLinkedinLink().trim().isEmpty())
                && (user.getPortfolioLink() == null || user.getPortfolioLink().trim().isEmpty())) {

            return ResponseEntity.badRequest()
                    .body("At least one of GitHub, LinkedIn or Portfolio link is required");
        }

        // Check if email already exists
        boolean emailExists = userRepository.findAll()
                .stream()
                .anyMatch(existingUser ->
                        existingUser.getEmail() != null
                                && existingUser.getEmail().equalsIgnoreCase(user.getEmail()));

        if (emailExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already registered");
        }

        // Set default values
        user.setCreatedAt(LocalDateTime.now());

        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("ACTIVE");
        }

        // Save user
        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // GET ALL USERS
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        List<User> users = userRepository.findAll();

        return ResponseEntity.ok(users);
    }

    // GET USER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {

        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        return ResponseEntity.ok(user.get());
    }

    // UPDATE USER PROFILE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody User updatedUser) {

        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        User user = optionalUser.get();

        // Update basic profile information
        if (updatedUser.getFirstName() != null
                && !updatedUser.getFirstName().trim().isEmpty()) {

            user.setFirstName(updatedUser.getFirstName());
        }

        if (updatedUser.getLastName() != null
                && !updatedUser.getLastName().trim().isEmpty()) {

            user.setLastName(updatedUser.getLastName());
        }

        if (updatedUser.getBio() != null) {
            user.setBio(updatedUser.getBio());
        }

        if (updatedUser.getGithubLink() != null) {
            user.setGithubLink(updatedUser.getGithubLink());
        }

        if (updatedUser.getLinkedinLink() != null) {
            user.setLinkedinLink(updatedUser.getLinkedinLink());
        }

        if (updatedUser.getPortfolioLink() != null) {
            user.setPortfolioLink(updatedUser.getPortfolioLink());
        }

        if (updatedUser.getProfileImage() != null) {
            user.setProfileImage(updatedUser.getProfileImage());
        }

        if (updatedUser.getStatus() != null) {
            user.setStatus(updatedUser.getStatus());
        }

        // Make sure at least one profile link remains
        if ((user.getGithubLink() == null || user.getGithubLink().trim().isEmpty())
                && (user.getLinkedinLink() == null || user.getLinkedinLink().trim().isEmpty())
                && (user.getPortfolioLink() == null || user.getPortfolioLink().trim().isEmpty())) {

            return ResponseEntity.badRequest()
                    .body("At least one of GitHub, LinkedIn or Portfolio link is required");
        }

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }

    // DELETE USER
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {

        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        userRepository.deleteById(id);

        return ResponseEntity.ok("User deleted successfully");
    }
}