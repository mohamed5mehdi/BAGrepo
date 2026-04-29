package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.BudgetFamille;
import com.pfe.gestionsachat.repository.BudgetFamilleRepository;
import com.pfe.gestionsachat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private BudgetFamilleRepository budgetFamilleRepository;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/budgets")
    public ResponseEntity<BudgetFamille> updateBudget(@RequestBody BudgetFamille budget) {
        return ResponseEntity.ok(budgetFamilleRepository.save(budget));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.getAllUsers().size());
        stats.put("totalBudgets", budgetFamilleRepository.count());
        // Add more KPIs
        return ResponseEntity.ok(stats);
    }
}
