package com.faro.admin;

import static com.faro.TestTokens.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.faro.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class PlanAdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    private ResultActions inviter() throws Exception {
        return mockMvc.perform(post("/api/entreprise/invitations").with(user("patron"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"CHARGE_ETUDES\"}"));
    }

    @Test
    void aNewCompanyIsOnTrialAndLimitedInUsers() throws Exception {
        mockMvc.perform(get("/api/entreprise").with(user("patron")))
            .andExpect(jsonPath("$.offre.plan").value("ESSAI"))
            .andExpect(jsonPath("$.offre.maxUtilisateurs").value(3))
            .andExpect(jsonPath("$.offre.essaiFin").isNotEmpty());
        inviter().andExpect(status().isCreated());
        inviter().andExpect(status().isCreated());
        inviter().andExpect(status().isConflict());
    }

    @Test
    void theSaasAdminSeesCompaniesAndChangesTheirPlan() throws Exception {
        mockMvc.perform(get("/api/entreprise").with(user("patron")));
        mockMvc.perform(post("/api/affaires").with(user("patron")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\": \"Maison\", \"typeBien\": \"MAISON\"}"));

        mockMvc.perform(get("/api/admin/organisations").with(user("patron")))
            .andExpect(status().isForbidden());
        String liste = mockMvc.perform(get("/api/admin/organisations")
                .with(jwt().jwt(t -> t.subject("proprietaire")).authorities(new SimpleGrantedAuthority("ROLE_admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].utilisateurs").value(1))
            .andExpect(jsonPath("$[0].affaires").value(1))
            .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(liste, "$[0].id");

        mockMvc.perform(put("/api/admin/organisations/" + id + "/plan")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"plan\": \"BUSINESS\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/entreprise").with(user("patron")))
            .andExpect(jsonPath("$.offre.plan").value("BUSINESS"))
            .andExpect(jsonPath("$.offre.maxUtilisateurs").doesNotExist())
            .andExpect(jsonPath("$.offre.essaiFin").doesNotExist());
        for (int i = 0; i < 4; i++) {
            inviter().andExpect(status().isCreated());
        }
    }
}
