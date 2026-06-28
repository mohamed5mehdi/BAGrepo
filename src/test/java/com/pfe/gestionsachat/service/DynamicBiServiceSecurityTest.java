package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.Role;
import com.pfe.gestionsachat.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DynamicBiServiceSecurityTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Query query;

    @InjectMocks
    private DynamicBiService dynamicBiService;

    private User mockAdminUser;

    @BeforeEach
    void setUp() {
        mockAdminUser = new User();
        mockAdminUser.setOidUser(1);
        mockAdminUser.setRole(Role.ADMINISTRATEUR);
        mockAdminUser.setService("IT");
    }

    @Test
    void executeDynamicBiQuery_ValidSelect_ShouldPassValidation() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockAdminUser));
        when(entityManager.createNativeQuery(anyString(), eq(jakarta.persistence.Tuple.class))).thenReturn(query);
        
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            dynamicBiService.executeDynamicBiQuery("SELECT * FROM budget_pieces", 1);
        });

        // Verify the native query was actually created
        verify(entityManager).createNativeQuery(contains("SELECT * FROM (SELECT * FROM budget_pieces) AS llm_result"), eq(jakarta.persistence.Tuple.class));
    }

    @Test
    void executeDynamicBiQuery_WithClauseUpdate_ShouldThrowSecurityException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockAdminUser));
        
        String maliciousSql = "WITH updated AS (UPDATE users SET role='ADMINISTRATEUR' RETURNING *) SELECT * FROM updated";
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            dynamicBiService.executeDynamicBiQuery(maliciousSql, 1);
        });

        assertTrue(exception.getMessage().contains("doit obligatoirement être un SELECT racine"));
        verify(entityManager, never()).createNativeQuery(anyString(), any(Class.class));
    }

    @Test
    void executeDynamicBiQuery_StartWithComment_ShouldThrowSecurityException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockAdminUser));
        
        String maliciousSql = "-- Bypass security\nSELECT * FROM users";
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            dynamicBiService.executeDynamicBiQuery(maliciousSql, 1);
        });

        assertTrue(exception.getMessage().contains("doit obligatoirement être un SELECT racine"));
        verify(entityManager, never()).createNativeQuery(anyString(), any(Class.class));
    }

    @Test
    void executeDynamicBiQuery_StackedQueries_ShouldThrowSecurityException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockAdminUser));
        
        String maliciousSql = "SELECT * FROM budget_pieces; DROP TABLE users";
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            dynamicBiService.executeDynamicBiQuery(maliciousSql, 1);
        });

        assertTrue(exception.getMessage().contains("requêtes empilées (;) sont interdites"));
        verify(entityManager, never()).createNativeQuery(anyString(), any(Class.class));
    }
}
