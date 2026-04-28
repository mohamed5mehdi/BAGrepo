package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ControllerMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    @Test
    void testCreateDaHeader_JsonMapping() throws Exception {
        User d = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_DEMANDEUR)
                .findFirst().orElseThrow();
        SubFamily sf = subFamilyRepository.findAll().get(0);

        String json = "{" +
                "\"objet\": \"Test JSON Mapping\"," +
                "\"demandeur\": {\"oid_user\": " + d.getOidUser() + "}," +
                "\"details\": [" +
                "  {" +
                "    \"quantite\": 2," +
                "    \"description\": \"Item Test\"," +
                "    \"prix_unitaire\": 150.0," +
                "    \"subFamily\": {\"oid_sub\": " + sf.getOidSub() + "}" +
                "  }" +
                "]" +
                "}";

        java.util.Objects.requireNonNull(mockMvc).perform(post("/api/da")
                .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objet").value("Test JSON Mapping"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_N1"));
    }

    @Test
    void testGetSubFamiliesByFamily() throws Exception {
        java.util.Objects.requireNonNull(mockMvc).perform(get("/api/sub-families/family/1"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    String content = result.getResponse().getContentAsString();
                    System.out.println("DEBUG: SubFamilies Response = " + content);
                });
    }
}
