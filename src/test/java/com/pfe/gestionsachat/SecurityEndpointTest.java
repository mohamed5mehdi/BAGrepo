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
        // Vérifie que les endpoints d'auth ne sont pas bloqués par un 401/403
        mockMvc.perform(get("/api/auth/login?email=test&password=test"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    assert statusCode != 403 : "Security bloquée sur un endpoint public (auth/login)";
                });
    }
}
