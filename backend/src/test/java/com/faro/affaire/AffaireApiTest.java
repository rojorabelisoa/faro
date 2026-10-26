package com.faro.affaire;

import static com.faro.TestTokens.user;
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

import com.faro.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class AffaireApiTest {

    private static final String AFFAIRE_JSON = """
        {"nom": "Rénovation T3 rue Victor Hugo", "clientNom": "M. Martin",
         "clientEmail": "martin@example.com", "adresse": "12 rue Victor Hugo, Lyon",
         "typeBien": "APPARTEMENT"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    private String create(String subject) throws Exception {
        String body = mockMvc.perform(post("/api/affaires").with(user(subject))
                .contentType(MediaType.APPLICATION_JSON).content(AFFAIRE_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nom").value("Rénovation T3 rue Victor Hugo"))
            .andExpect(jsonPath("$.statut").value("VISITE"))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void createListUpdateAndDeleteAnAffaire() throws Exception {
        String id = create("affaire-crud");

        mockMvc.perform(get("/api/affaires").with(user("affaire-crud")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].clientNom").value("M. Martin"));

        mockMvc.perform(put("/api/affaires/" + id).with(user("affaire-crud"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nom": "Rénovation T3", "typeBien": "APPARTEMENT", "statut": "ESTIMATION"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("Rénovation T3"))
            .andExpect(jsonPath("$.statut").value("ESTIMATION"))
            .andExpect(jsonPath("$.clientNom").doesNotExist());

        mockMvc.perform(delete("/api/affaires/" + id).with(user("affaire-crud")))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + id).with(user("affaire-crud")))
            .andExpect(status().isNotFound());
    }

    @Test
    void anotherOrganisationCannotSeeOrChangeTheAffaire() throws Exception {
        String id = create("org-a");

        mockMvc.perform(get("/api/affaires").with(user("org-b")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/affaires/" + id).with(user("org-b")))
            .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/affaires/" + id).with(user("org-b"))
                .contentType(MediaType.APPLICATION_JSON).content(AFFAIRE_JSON))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/affaires/" + id).with(user("org-b")))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidAffaire() throws Exception {
        mockMvc.perform(post("/api/affaires").with(user("affaire-invalid"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nom": "", "clientEmail": "pas-un-email"}
                    """))
            .andExpect(status().isBadRequest());
    }
}
