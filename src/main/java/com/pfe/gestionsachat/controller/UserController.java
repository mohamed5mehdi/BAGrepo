package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.Role;
import com.pfe.gestionsachat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<User> createUser(@RequestBody @org.springframework.lang.NonNull User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<User> getUserById(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable @org.springframework.lang.NonNull Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<User> updateUser(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull User userDetails) {
        return ResponseEntity.ok(userService.updateUser(id, userDetails));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteUser(@PathVariable @org.springframework.lang.NonNull Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
