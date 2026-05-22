package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.Role;
import com.pfe.gestionsachat.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;

    @Transactional
    public User createUser(@NonNull User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @NonNull
    public User getUserById(@NonNull Integer id) {
        return java.util.Objects.requireNonNull(userRepository.findByIdWithWarehouse(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé")));
    }

    @NonNull
    public User getUserByEmail(@NonNull String email) {
        return java.util.Objects.requireNonNull(userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé")));
    }

    public List<User> getUsersByRole(@NonNull Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User updateUser(@NonNull Integer id, @NonNull User userDetails) {
        User user = getUserById(id);
        user.setNom(userDetails.getNom());
        user.setEmail(userDetails.getEmail());
        user.setRole(userDetails.getRole());
        user.setActif(userDetails.getActif());
        user.setWarehouse(userDetails.getWarehouse()); // assignation entrepôt magasinier
        return userRepository.save(user);
    }

    @Transactional
    public User changePassword(@NonNull Integer id, @NonNull String newPassword) {
        User user = getUserById(id);
        user.setPassword(encoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(@NonNull Integer id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchByKeyword(keyword);
    }

    @NonNull
    public User authenticate(@NonNull String email, @NonNull String password) {
        User user = getUserByEmail(email);
        if (encoder.matches(password, user.getPassword())) {
            return user;
        }
        throw new RuntimeException("Mot de passe incorrect");
    }
}