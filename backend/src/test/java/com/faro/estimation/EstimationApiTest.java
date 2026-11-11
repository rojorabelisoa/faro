package com.faro.estimation;

import static com.faro.TestTokens.user;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
class EstimationApiTest {

    private static final String SUB = "estimation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private String cuisine;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        affaireId = JsonPath.read(post("/api/affaires", """
            {"nom": "T3", "typeBien": "APPARTEMENT"}
            """), "$.id");
        cuisine = JsonPath.read(post("/api/affaires/" + affaireId + "/pieces", """
            {"nom": "Cuisine", "surfaceM2": 10, "typePiece": "CUISINE", "niveauTravaux": "RENOVATION"}
            """), "$.id");
        post("/api/affaires/" + affaireId + "/pieces", """
            {"nom": "Chambre", "surfaceM2": 12, "typePiece": "CHAMBRE", "niveauTravaux": "RAFRAICHISSEMENT"}
            """);
        post("/api/affaires/" + affaireId + "/pieces", """
            {"nom": "SDB", "typePiece": "SALLE_DE_BAINS", "niveauTravaux": "RENOVATION"}
            """);
    }

    private String post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .with(user(SUB)).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    }

    private String ratioId(String type, String niveau) throws Exception {
        String json = mockMvc.perform(get("/api/ratios").with(user(SUB))).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(json,
            "$[?(@.typePiece == '" + type + "' && @.niveauTravaux == '" + niveau + "')].id");
        return ids.get(0);
    }

    @Test
    void estimatesARangeFromSurfacesAndRatios() throws Exception {
        mockMvc.perform(get("/api/affaires/" + affaireId + "/estimation").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pieces", hasSize(3)))
            .andExpect(jsonPath("$.pieces[0].minHt").value(7000.00))
            .andExpect(jsonPath("$.pieces[0].maxHt").value(13000.00))
            .andExpect(jsonPath("$.pieces[1].minHt").value(480.00))
            .andExpect(jsonPath("$.pieces[2].manque").value("surface"))
            .andExpect(jsonPath("$.totalMinHt").value(7480.00))
            .andExpect(jsonPath("$.totalMaxHt").value(13960.00))
            .andExpect(jsonPath("$.devisHt").value(0));
    }

    @Test
    void ratiosCanBeAdjustedAndShowWhatTheDevisActuallyCost() throws Exception {
        mockMvc.perform(get("/api/ratios").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(24)));

        String ratio = ratioId("CUISINE", "RENOVATION");
        mockMvc.perform(put("/api/ratios/" + ratio).with(user(SUB)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"prixMinM2": 1200, "prixMaxM2": 900}
                    """))
            .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/ratios/" + ratio).with(user(SUB)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"prixMinM2": 800, "prixMaxM2": 1100}
                    """))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/estimation").with(user(SUB)))
            .andExpect(jsonPath("$.pieces[0].minHt").value(8000.00))
            .andExpect(jsonPath("$.totalMaxHt").value(11960.00));

        String lots = mockMvc.perform(get("/api/lots").with(user(SUB))).andReturn().getResponse().getContentAsString();
        String lot = JsonPath.read(lots, "$[0].id");
        post("/api/affaires/" + affaireId + "/lignes", """
            {"pieceId": "%s", "lotId": "%s", "designation": "Cuisine complète", "unite": "forfait",
             "quantite": 1, "prixUnitaireHt": 9500}
            """.formatted(cuisine, lot));

        mockMvc.perform(get("/api/ratios").with(user(SUB)))
            .andExpect(jsonPath("$[?(@.id == '" + ratio + "')].constate.pieces").value(1))
            .andExpect(jsonPath("$[?(@.id == '" + ratio + "')].constate.medianeM2").value(950.00));
        mockMvc.perform(get("/api/affaires/" + affaireId + "/estimation").with(user(SUB)))
            .andExpect(jsonPath("$.devisHt").value(9500.00));

        mockMvc.perform(put("/api/ratios/" + ratio).with(user("estimation-autre"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"prixMinM2": 1, "prixMaxM2": 2}
                    """))
            .andExpect(status().isNotFound());
    }
}
