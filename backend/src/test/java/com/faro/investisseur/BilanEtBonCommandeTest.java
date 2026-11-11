package com.faro.investisseur;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class BilanEtBonCommandeTest {

    private static final String SUB = "investisseur";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private final Map<String, String> lots = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        affaireId = JsonPath.read(post("/api/affaires", """
            {"nom": "Immeuble rue Royale", "typeBien": "APPARTEMENT", "adresse": "3 rue Royale, Lyon"}
            """), "$.id");
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

    private String ligne(String lot, String designation, String quantite, String prix, String gamme) {
        return """
            {"lotId": "%s", "designation": "%s", "unite": "m²", "quantite": %s, "prixUnitaireHt": %s, "gamme": %s}
            """.formatted(lots.get(lot), designation, quantite, prix, gamme == null ? "null" : "\"" + gamme + "\"");
    }

    @Test
    void computesTheOperationBalanceAndRiskScenarios() throws Exception {
        post("/api/affaires/" + affaireId + "/lignes", ligne("03", "Rénovation complète", "1", "20000", null));
        mockMvc.perform(get("/api/affaires/" + affaireId + "/bilan").with(user(SUB)))
            .andExpect(jsonPath("$.hypotheses").doesNotExist())
            .andExpect(jsonPath("$.travauxTtc").value(22000.00))
            .andExpect(jsonPath("$.scenarios", hasSize(0)));

        mockMvc.perform(put("/api/affaires/" + affaireId + "/bilan").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"prixAchat": 150000, "fraisAcquisition": 12000, "autresFrais": 3000, "prixRevente": 230000,
                     "dureeMois": 12}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarios", hasSize(3)))
            .andExpect(jsonPath("$.scenarios[0].coutTotal").value(187000.00))
            .andExpect(jsonPath("$.scenarios[0].margeBrute").value(43000.00))
            .andExpect(jsonPath("$.scenarios[0].rentabilite").value(23.0))
            .andExpect(jsonPath("$.scenarios[0].triAnnuel").value(23.0))
            .andExpect(jsonPath("$.scenarios[1].travauxTtc").value(24200.00))
            .andExpect(jsonPath("$.scenarios[1].margeBrute").value(40800.00))
            .andExpect(jsonPath("$.scenarios[1].rentabilite").value(21.6))
            .andExpect(jsonPath("$.scenarios[2].dureeMois").value(15))
            .andExpect(jsonPath("$.scenarios[2].triAnnuel").value(18.0));

        mockMvc.perform(get("/api/affaires/" + affaireId + "/bilan").with(user("investisseur-autre")))
            .andExpect(status().isNotFound());
    }

    @Test
    void thePurchaseOrderListsTheRetainedWorkOfTheLot() throws Exception {
        post("/api/affaires/" + affaireId + "/lignes", ligne("10", "Carrelage 60x60", "18.5", "70", null));
        post("/api/affaires/" + affaireId + "/lignes", ligne("10", "Zellige", "18.5", "300", "PREMIUM"));
        post("/api/affaires/" + affaireId + "/lignes", ligne("07", "WC suspendu", "1", "700", null));
        post("/api/affaires/" + affaireId + "/chantier", "{}");
        String avenant = JsonPath.read(post("/api/affaires/" + affaireId + "/avenants", "{\"objet\": \"Plinthes\"}"), "$.id");
        post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/lignes", ligne("10", "Plinthes carrelage", "22", "15", null));
        post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/envoyer", "");
        post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/decision", "{\"decision\": \"VALIDE\", \"par\": \"Client\"}");

        byte[] pdf = mockMvc.perform(get("/api/affaires/" + affaireId + "/lots/" + lots.get("10") + "/bon-commande.pdf")
                .param("fournisseur", "Point P").with(user(SUB)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String texte = new org.openpdf.text.pdf.parser.PdfTextExtractor(new org.openpdf.text.pdf.PdfReader(pdf))
            .getTextFromPage(1);
        assertThat(texte).contains("Bon de commande — 10 · Carrelage / Faïence", "Fournisseur : Point P",
            "livraison : 3 rue Royale, Lyon", "Carrelage 60x60", "18,5 m²", "Plinthes carrelage", "22 m²")
            .doesNotContain("Zellige", "WC suspendu", "70,00");

        mockMvc.perform(get("/api/affaires/" + affaireId + "/lots/" + lots.get("10") + "/bon-commande.pdf")
                .with(user("investisseur-autre")))
            .andExpect(status().isNotFound());
    }
}
