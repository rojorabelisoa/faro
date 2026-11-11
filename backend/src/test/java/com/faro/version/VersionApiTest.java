package com.faro.version;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
class VersionApiTest {

    private static final String SUB = "versions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private String ligneId;
    private String lotId;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        affaireId = JsonPath.read(postJson("/api/affaires", """
            {"nom": "T2 Part-Dieu", "typeBien": "APPARTEMENT", "clientNom": "Mme Roux"}
            """), "$.id");
        lotId = JsonPath.read(mockMvc.perform(get("/api/lots").with(user(SUB))).andReturn().getResponse()
            .getContentAsString(), "$[11].id");
        ligneId = JsonPath.read(postJson("/api/affaires/" + affaireId + "/lignes", """
            {"lotId": "%s", "designation": "Peinture séjour", "unite": "m²", "quantite": 40, "prixUnitaireHt": 25}
            """.formatted(lotId)), "$.id");
    }

    private String postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).with(user(SUB)).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    }

    private static String texte(byte[] pdf) throws Exception {
        return new org.openpdf.text.pdf.parser.PdfTextExtractor(new org.openpdf.text.pdf.PdfReader(pdf)).getTextFromPage(1);
    }

    @Test
    void aVersionKeepsTheDevisAsItWasWhenFrozen() throws Exception {
        String version = JsonPath.read(postJson("/api/affaires/" + affaireId + "/versions", """
            {"libelle": "Devis envoyé au client"}
            """), "$.id");

        mockMvc.perform(put("/api/affaires/" + affaireId + "/lignes/" + ligneId).with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"lotId": "%s", "designation": "Peinture séjour et couloir", "unite": "m²", "quantite": 55,
                     "prixUnitaireHt": 25}
                    """.formatted(lotId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/versions/" + version + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.totalHt").value(1000.00))
            .andExpect(jsonPath("$.lignes[0].designation").value("Peinture séjour"));
        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.totalHt").value(1375.00));

        String pdf = texte(mockMvc.perform(get("/api/affaires/" + affaireId + "/versions/" + version + "/devis.pdf")
                .with(user(SUB)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertThat(pdf).contains("Devis envoyé au client (version 1)", "Peinture séjour", "1 100,00 €", "Mme Roux")
            .doesNotContain("couloir");

        postJson("/api/affaires/" + affaireId + "/versions", "{}");
        mockMvc.perform(get("/api/affaires/" + affaireId + "/versions").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].numero").value(2))
            .andExpect(jsonPath("$[0].libelle").value("Version 2"))
            .andExpect(jsonPath("$[0].totalTtc").value(1512.50))
            .andExpect(jsonPath("$[1].auteur").value(SUB));
    }

    @Test
    void passingToChantierFreezesTheWorkFileOnce() throws Exception {
        String version = JsonPath.read(postJson("/api/affaires/" + affaireId + "/chantier", "{}"), "$.id");

        mockMvc.perform(get("/api/affaires/" + affaireId).with(user(SUB)))
            .andExpect(jsonPath("$.statut").value("CHANTIER"))
            .andExpect(jsonPath("$.versionChantierId").value(version));
        mockMvc.perform(get("/api/affaires/" + affaireId + "/versions").with(user(SUB)))
            .andExpect(jsonPath("$[0].libelle").value("Dossier travaux"))
            .andExpect(jsonPath("$[0].chantier").value(true));
        mockMvc.perform(post("/api/affaires/" + affaireId + "/chantier").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
    }

    @Test
    void anEmptyDevisCannotBeFrozenAndOtherOrganisationsSeeNothing() throws Exception {
        String vide = JsonPath.read(postJson("/api/affaires", """
            {"nom": "Vide", "typeBien": "MAISON"}
            """), "$.id");
        mockMvc.perform(post("/api/affaires/" + vide + "/versions").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        String version = JsonPath.read(postJson("/api/affaires/" + affaireId + "/versions", "{}"), "$.id");
        mockMvc.perform(get("/api/affaires/" + affaireId + "/versions/" + version + "/devis").with(user("versions-autre")))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/versions").with(user("versions-autre")))
            .andExpect(status().isNotFound());
    }
}
