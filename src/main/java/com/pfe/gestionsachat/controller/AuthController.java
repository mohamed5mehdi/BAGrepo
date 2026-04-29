package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.Role;
import com.pfe.gestionsachat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String email,
            @RequestParam String password) {

        String cleanEmail = email.trim().toLowerCase();
        Map<String, Object> response = new HashMap<>();

        // Chercher dans User
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (encoder.matches(password, user.getPassword())) {
                return buildSuccessResponse(user.getOidUser(), user.getNom(), user.getEmail(), user.getRole());
            }
        }

        response.put("success", false);
        response.put("message", "Invalid email or password");
        return ResponseEntity.status(401).body(response);
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(Integer id, String nom, String email, Role role) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Login successful");
        response.put("userId", id);
        response.put("userName", nom);
        response.put("email", email);
        response.put("role", role);
        return ResponseEntity.ok(response);
    }
}