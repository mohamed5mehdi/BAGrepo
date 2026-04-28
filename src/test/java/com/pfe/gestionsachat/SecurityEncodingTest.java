package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityEncodingTest {

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testPasswordEncoding() {
        String rawPassword = "password123";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("Raw: " + rawPassword);
        System.out.println("Encoded: " + encodedPassword);
        
        assertTrue(encoder.matches(rawPassword, encodedPassword));
        assertNotEquals(rawPassword, encodedPassword);
    }

    @Test
    void testExistingUserPasswordsAreEncoded() {
        User demandeur = userRepository.findByEmail("demandeur@test.com").orElseThrow();
        String password = demandeur.getPassword();
        
        System.out.println("Demandeur Password Hash: " + password);
        
        // BCrypt hashes start with $2a$, $2b$ or $2y$
        assertTrue(password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"), 
            "Password should be BCrypt encoded");
            
        assertTrue(encoder.matches("password", password), "Password should match 'password'");
    }
}
