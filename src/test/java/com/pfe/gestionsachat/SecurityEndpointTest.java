package com.pfe.gestionsachat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPublicEndpointsAreAccessible() throws Exception {
        // Vérifie que les endpoints de base ne sont pas bloqués par un 401/403
        // (Comme défini dans le SecurityConfig avec permitAll)
        mockMvc.perform(get("/api/users/all"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    assert statusCode != 401 && statusCode != 403 : "Security bloquée sur un endpoint censé être public";
                });
    }
}
