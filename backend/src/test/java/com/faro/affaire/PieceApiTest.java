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
class PieceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    private String createAffaire(String subject) throws Exception {
        String body = mockMvc.perform(post("/api/affaires").with(user(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nom": "Maison Dupont", "typeBien": "MAISON"}
                    """))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String createPiece(String subject, String affaireId, String json) throws Exception {
        String body = mockMvc.perform(post("/api/affaires/" + affaireId + "/pieces").with(user(subject))
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void piecesAreOrderedInCreationOrder() throws Exception {
        String affaireId = createAffaire("pieces");
        createPiece("pieces", affaireId, """
            {"nom": "Cuisine", "surfaceM2": 12.5}
            """);
        String sdb = createPiece("pieces", affaireId, """
            {"nom": "Salle de bains"}
            """);

        mockMvc.perform(get("/api/affaires/" + affaireId + "/pieces").with(user("pieces")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].nom").value("Cuisine"))
            .andExpect(jsonPath("$[0].surfaceM2").value(12.5))
            .andExpect(jsonPath("$[1].ordre").value(2));

        mockMvc.perform(put("/api/affaires/" + affaireId + "/pieces/" + sdb).with(user("pieces"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nom": "SDB étage", "surfaceM2": 6, "ordre": 0}
                    """))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/pieces").with(user("pieces")))
            .andExpect(jsonPath("$[0].nom").value("SDB étage"));

        mockMvc.perform(delete("/api/affaires/" + affaireId + "/pieces/" + sdb).with(user("pieces")))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/pieces").with(user("pieces")))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deletingTheAffaireDeletesItsPieces() throws Exception {
        String affaireId = createAffaire("pieces-cascade");
        createPiece("pieces-cascade", affaireId, """
            {"nom": "Séjour"}
            """);

        mockMvc.perform(delete("/api/affaires/" + affaireId).with(user("pieces-cascade")))
            .andExpect(status().isNoContent());
    }

    @Test
    void anotherOrganisationCannotSeeOrAddPieces() throws Exception {
        String affaireId = createAffaire("pieces-a");

        mockMvc.perform(get("/api/affaires/" + affaireId + "/pieces").with(user("pieces-b")))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/affaires/" + affaireId + "/pieces").with(user("pieces-b"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nom": "Intrus"}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsNegativeSurface() throws Exception {
        String affaireId = createAffaire("pieces-invalide");
        mockMvc.perform(post("/api/affaires/" + affaireId + "/pieces").with(user("pieces-invalide"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nom": "Cave", "surfaceM2": -3}
                    """))
            .andExpect(status().isBadRequest());
    }
}
