package com.faro.marge;

import static com.faro.TestTokens.user;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
class MargeApiTest {

    private static final String SUB = "marge";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private final Map<String, String> lots = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        affaireId = id(post("/api/affaires", """
            {"nom": "Maison Blanc", "typeBien": "MAISON"}
            """));
        List<Map<String, Object>> liste = JsonPath.read(mockMvc.perform(get("/api/lots").with(user(SUB)))
            .andReturn().getResponse().getContentAsString(), "$");
        liste.forEach(lot -> lots.put((String) lot.get("code"), (String) lot.get("id")));
    }

    private String post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .with(user(SUB)).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }

    private String ligne(String lot, String designation, String quantite, String prix, String debourse, String gamme) {
        return """
            {"lotId": "%s", "designation": "%s", "unite": "U", "quantite": %s, "prixUnitaireHt": %s,
             "debourseUnitaireHt": %s, "gamme": %s}
            """.formatted(lots.get(lot), designation, quantite, prix, debourse, gamme == null ? "null" : "\"" + gamme + "\"");
    }

    private void depense(String lot, String nature, String libelle, String montant) throws Exception {
        post("/api/affaires/" + affaireId + "/depenses", """
            {"lotId": "%s", "nature": "%s", "libelle": "%s", "montantHt": %s, "dateDepense": "2026-11-02"}
            """.formatted(lots.get(lot), nature, libelle, montant));
    }

    @Test
    void comparesPlannedAndActualMarginPerLot() throws Exception {
        String base = "/api/affaires/" + affaireId + "/lignes";
        post(base, ligne("07", "WC suspendu", "1", "700", "420", null));
        post(base, ligne("07", "Évier", "2", "150", "null", null));
        post(base, ligne("10", "Carrelage", "10", "70", "45", null));
        post(base, ligne("10", "Carrelage premium", "10", "120", "80", "PREMIUM"));
        post("/api/affaires/" + affaireId + "/chantier", "{}");

        String valide = id(post("/api/affaires/" + affaireId + "/avenants", "{\"objet\": \"Plinthes\"}"));
        post("/api/affaires/" + affaireId + "/avenants/" + valide + "/lignes", ligne("10", "Plinthes", "1", "200", "120", null));
        post("/api/affaires/" + affaireId + "/avenants/" + valide + "/envoyer", "");
        post("/api/affaires/" + affaireId + "/avenants/" + valide + "/decision", "{\"decision\": \"VALIDE\", \"par\": \"M. Blanc\"}");
        String refuse = id(post("/api/affaires/" + affaireId + "/avenants", "{\"objet\": \"Terrasse\"}"));
        post("/api/affaires/" + affaireId + "/avenants/" + refuse + "/lignes", ligne("03", "Terrasse", "1", "5000", "3000", null));
        post("/api/affaires/" + affaireId + "/avenants/" + refuse + "/envoyer", "");
        post("/api/affaires/" + affaireId + "/avenants/" + refuse + "/decision", "{\"decision\": \"REFUSE\", \"par\": \"M. Blanc\"}");

        depense("07", "MATERIAUX", "Facture Cedeo", "480");
        depense("10", "MATERIAUX", "Carrelage Point P", "300");
        depense("10", "SOUS_TRAITANCE", "Carreleur", "200");
        depense("08", "AUTRE", "Petites fournitures", "50");

        mockMvc.perform(get("/api/affaires/" + affaireId + "/marge").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lots", hasSize(3)))
            .andExpect(jsonPath("$.lots[0].code").value("07"))
            .andExpect(jsonPath("$.lots[0].venduHt").value(1000.00))
            .andExpect(jsonPath("$.lots[0].coutPrevuHt").value(420.00))
            .andExpect(jsonPath("$.lots[0].coutReelHt").value(480.00))
            .andExpect(jsonPath("$.lots[0].margeReelleHt").value(520.00))
            .andExpect(jsonPath("$.lots[0].lignesSansDebourse").value(1))
            .andExpect(jsonPath("$.lots[1].code").value("08"))
            .andExpect(jsonPath("$.lots[1].margeReelleHt").value(-50.00))
            .andExpect(jsonPath("$.lots[2].venduHt").value(900.00))
            .andExpect(jsonPath("$.lots[2].coutPrevuHt").value(570.00))
            .andExpect(jsonPath("$.lots[2].margePrevueHt").value(330.00))
            .andExpect(jsonPath("$.total.venduHt").value(1900.00))
            .andExpect(jsonPath("$.total.coutPrevuHt").value(990.00))
            .andExpect(jsonPath("$.total.coutReelHt").value(1030.00))
            .andExpect(jsonPath("$.total.margeReelleHt").value(870.00))
            .andExpect(jsonPath("$.total.tauxMargePrevue").value(47.9))
            .andExpect(jsonPath("$.total.tauxMargeReelle").value(45.8))
            .andExpect(jsonPath("$.depenses", hasSize(4)));
    }

    @Test
    void expensesAreScopedToTheOrganisation() throws Exception {
        depense("07", "MATERIAUX", "Facture", "100");
        String depense = JsonPath.read(mockMvc.perform(get("/api/affaires/" + affaireId + "/marge").with(user(SUB)))
            .andReturn().getResponse().getContentAsString(), "$.depenses[0].id");

        mockMvc.perform(get("/api/affaires/" + affaireId + "/marge").with(user("marge-autre")))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/affaires/" + affaireId + "/depenses/" + depense).with(user("marge-autre")))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/affaires/" + affaireId + "/depenses/" + depense).with(user(SUB)))
            .andExpect(status().isNoContent());
    }
}
