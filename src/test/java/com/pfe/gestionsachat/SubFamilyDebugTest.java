package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.repository.FamilyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMINISTRATEUR")
public class SubFamilyDebugTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyRepository familyRepository;

    @Test
    void debugSubFamiliesByFamilyEndpoint() throws Exception {
        Family f = familyRepository.findAll().get(0);
        System.out.println("DEBUG: Testing Family ID = " + f.getIdFamily() + " (" + f.getLibelle() + ")");

        mockMvc.perform(get("/api/sub-families/family/" + f.getIdFamily()))
                .andExpect(status().isOk())
                .andDo(result -> {
                    String content = result.getResponse().getContentAsString();
                    System.out.println("DEBUG: JSON RESPONSE CONTENT (ID) = " + content);
                });

        mockMvc.perform(get("/api/sub-families/" + f.getIdFamily()))
                .andExpect(status().isOk())
                .andDo(result -> {
                    String content = result.getResponse().getContentAsString();
                    System.out.println("DEBUG: JSON RESPONSE CONTENT (DIRECT ID) = " + content);
                });
    }
}
