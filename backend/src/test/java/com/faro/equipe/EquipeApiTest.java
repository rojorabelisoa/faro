package com.faro.equipe;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.faro.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class EquipeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
            String sub, String json) throws Exception {
        return mockMvc.perform(requete.with(user(sub)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String inviter(String role) throws Exception {
        return JsonPath.read(send(post("/api/entreprise/invitations"), "patron", """
            {"role": "%s", "email": "collegue@renov.fr"}
            """.formatted(role)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.code");
    }

    @Test
    void theManagerInvitesAColleagueWhoThenSharesTheCompanyData() throws Exception {
        send(put("/api/entreprise"), "patron", "{\"nom\": \"Rénov Plus\"}").andExpect(status().isNoContent());
        send(post("/api/affaires"), "patron", "{\"nom\": \"Maison Leroy\", \"typeBien\": \"MAISON\"}")
            .andExpect(status().isCreated());
        String code = inviter("CHARGE_ETUDES");

        // La collègue se connecte une première fois (elle reçoit sa propre entreprise), puis accepte l'invitation.
        mockMvc.perform(get("/api/me").with(user("collegue"))).andExpect(jsonPath("$.organisation.nom").value("Mon entreprise"));
        mockMvc.perform(get("/api/invitations/" + code).with(user("collegue")))
            .andExpect(jsonPath("$.entreprise").value("Rénov Plus"))
            .andExpect(jsonPath("$.utilisable").value(true));
        mockMvc.perform(post("/api/invitations/" + code + "/accepter").with(user("collegue")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").with(user("collegue")))
            .andExpect(jsonPath("$.organisation.nom").value("Rénov Plus"))
            .andExpect(jsonPath("$.roleEntreprise").value("CHARGE_ETUDES"));
        mockMvc.perform(get("/api/affaires").with(user("collegue")))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nom").value("Maison Leroy"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organisation", Integer.class)).isEqualTo(1);
        mockMvc.perform(get("/api/entreprise").with(user("collegue")))
            .andExpect(jsonPath("$.membres", hasSize(2)));

        // Réservé au gérant ; invitation à usage unique.
        send(put("/api/entreprise"), "collegue", "{\"nom\": \"Pirate\"}").andExpect(status().isForbidden());
        send(post("/api/entreprise/invitations"), "collegue", "{\"role\": \"GERANT\"}").andExpect(status().isForbidden());
        mockMvc.perform(post("/api/invitations/" + code + "/accepter").with(user("intrus")))
            .andExpect(status().isGone());
    }

    @Test
    void theCompanyAlwaysKeepsAManager() throws Exception {
        mockMvc.perform(get("/api/me").with(user("patron")));
        String code = inviter("CONDUCTEUR_TRAVAUX");
        mockMvc.perform(post("/api/invitations/" + code + "/accepter").with(user("conducteur")))
            .andExpect(status().isNoContent());
        String membres = mockMvc.perform(get("/api/entreprise").with(user("patron"))).andReturn().getResponse()
            .getContentAsString();
        String patronId = JsonPath.<java.util.List<String>>read(membres, "$.membres[?(@.role == 'GERANT')].id").get(0);
        String conducteurId = JsonPath.<java.util.List<String>>read(membres, "$.membres[?(@.role == 'CONDUCTEUR_TRAVAUX')].id").get(0);

        send(put("/api/entreprise/membres/" + patronId + "/role"), "patron", "{\"role\": \"CHARGE_ETUDES\"}")
            .andExpect(status().isConflict());
        send(put("/api/entreprise/membres/" + conducteurId + "/role"), "patron", "{\"role\": \"GERANT\"}")
            .andExpect(status().isNoContent());
        send(put("/api/entreprise/membres/" + patronId + "/role"), "patron", "{\"role\": \"CHARGE_ETUDES\"}")
            .andExpect(status().isNoContent());

        // Le nouveau gérant retire l'ancien, qui retrouve une entreprise à lui.
        mockMvc.perform(delete("/api/entreprise/membres/" + patronId).with(user("conducteur")))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me").with(user("patron")))
            .andExpect(jsonPath("$.organisation.nom").value("Mon entreprise"))
            .andExpect(jsonPath("$.roleEntreprise").value("GERANT"));
    }

    @Test
    void invitationsCanBeListedAndRevoked() throws Exception {
        mockMvc.perform(get("/api/me").with(user("patron")));
        inviter("CHARGE_ETUDES");
        String invitations = mockMvc.perform(get("/api/entreprise/invitations").with(user("patron")))
            .andExpect(jsonPath("$", hasSize(1)))
            .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(invitations, "$[0].id");
        mockMvc.perform(delete("/api/entreprise/invitations/" + id).with(user("autre")))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/entreprise/invitations/" + id).with(user("patron")))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/entreprise/invitations").with(user("patron")))
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
