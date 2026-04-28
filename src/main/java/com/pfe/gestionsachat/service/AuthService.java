package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;

    public User login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (encoder.matches(password, user.getPassword())) {
                if (user.getActif() != null && !user.getActif()) {
                    throw new RuntimeException("Compte désactivé");
                }
                return user;
            }
        }
        
        throw new RuntimeException("Email ou mot de passe incorrect");
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}