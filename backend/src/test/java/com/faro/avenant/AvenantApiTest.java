package com.faro.avenant;

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
class AvenantApiTest {

    private static final String SUB = "avenants";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private String lotId;
    private String ligneBase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        affaireId = JsonPath.read(send(post("/api/affaires"), """
            {"nom": "Maison Roux", "typeBien": "MAISON", "clientNom": "Mme Roux"}
            """).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        lotId = JsonPath.read(mockMvc.perform(get("/api/lots").with(user(SUB))).andReturn().getResponse()
            .getContentAsString(), "$[7].id");
        ligneBase = JsonPath.read(send(post("/api/affaires/" + affaireId + "/lignes"), ligne("Tableau électrique", "1", "1800"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        send(post("/api/affaires/" + affaireId + "/chantier"), "{}").andExpect(status().isCreated());
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
            String json) throws Exception {
        return mockMvc.perform(requete.with(user(SUB)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String ligne(String designation, String quantite, String prix) {
        return """
            {"lotId": "%s", "designation": "%s", "unite": "U", "quantite": %s, "prixUnitaireHt": %s}
            """.formatted(lotId, designation, quantite, prix);
    }

    private String creerAvenant(String objet) throws Exception {
        return JsonPath.read(send(post("/api/affaires/" + affaireId + "/avenants"), """
            {"objet": "%s", "motif": "Demande du client pendant la visite de chantier"}
            """.formatted(objet)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void theBaseDevisIsFrozenOnceInChantier() throws Exception {
        send(post("/api/affaires/" + affaireId + "/lignes"), ligne("Prise", "1", "80")).andExpect(status().isConflict());
        send(put("/api/affaires/" + affaireId + "/lignes/" + ligneBase), ligne("Tableau", "1", "1"))
            .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/affaires/" + affaireId + "/lignes/" + ligneBase).with(user(SUB)))
            .andExpect(status().isConflict());
        send(put("/api/affaires/" + affaireId + "/lignes/ordre"), "{\"ids\": [\"" + ligneBase + "\"]}")
            .andExpect(status().isConflict());
    }

    @Test
    void anAvenantGoesFromDraftToSentToValidatedAndUpdatesTheMarket() throws Exception {
        String avenant = creerAvenant("Prises supplémentaires séjour");
        String prise = JsonPath.read(send(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/lignes"),
                ligne("Prise 16A", "4", "85")).andExpect(status().isCreated()).andReturn().getResponse()
            .getContentAsString(), "$.id");
        send(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/lignes"), ligne("Moins-value spot", "1", "-60"))
            .andExpect(status().isCreated());
        send(put("/api/affaires/" + affaireId + "/lignes/" + prise), ligne("Prise 16A", "5", "85"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/avenants").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].numero").value(1))
            .andExpect(jsonPath("$[0].statut").value("BROUILLON"))
            .andExpect(jsonPath("$[0].totaux.totalHt").value(365.00))
            .andExpect(jsonPath("$[0].totaux.totalTtc").value(401.50));
        // Les lignes d'avenant ne font pas partie du devis de base.
        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.lignes", hasSize(1)))
            .andExpect(jsonPath("$.totalHt").value(1800.00));

        mockMvc.perform(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/decision").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\": \"VALIDE\", \"par\": \"Mme Roux\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/envoyer").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("ENVOYE"));
        send(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/lignes"), ligne("Ajout", "1", "1"))
            .andExpect(status().isConflict());
        send(put("/api/affaires/" + affaireId + "/lignes/" + prise), ligne("Prise 16A", "9", "85"))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/marche").with(user(SUB)))
            .andExpect(jsonPath("$.chantier").value(true))
            .andExpect(jsonPath("$.initialHt").value(1800.00))
            .andExpect(jsonPath("$.avenantsEnAttenteHt").value(365.00))
            .andExpect(jsonPath("$.actuelHt").value(1800.00));

        send(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/decision"), """
            {"decision": "VALIDE", "par": "Mme Roux", "commentaire": "OK par téléphone"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("VALIDE"))
            .andExpect(jsonPath("$.decidePar").value("Mme Roux"));
        mockMvc.perform(get("/api/affaires/" + affaireId + "/marche").with(user(SUB)))
            .andExpect(jsonPath("$.avenantsValidesHt").value(365.00))
            .andExpect(jsonPath("$.actuelHt").value(2165.00))
            .andExpect(jsonPath("$.actuelTtc").value(2381.50))
            .andExpect(jsonPath("$.avenantsEnAttenteHt").value(0));

        byte[] pdf = mockMvc.perform(get("/api/affaires/" + affaireId + "/avenants/" + avenant + "/document.pdf")
                .with(user(SUB)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String texte = new org.openpdf.text.pdf.parser.PdfTextExtractor(new org.openpdf.text.pdf.PdfReader(pdf))
            .getTextFromPage(1);
        assertThat(texte).contains("Avenant n° 1 — Prises supplémentaires séjour", "Prise 16A", "Moins-value spot",
            "-60,00 €", "401,50 €", "VALIDÉ le", "par Mme Roux", "OK par téléphone");
    }

    @Test
    void aRefusedAvenantDoesNotCountAndAnEmptyOneCannotBeSent() throws Exception {
        String vide = creerAvenant("Vide");
        mockMvc.perform(post("/api/affaires/" + affaireId + "/avenants/" + vide + "/envoyer").with(user(SUB)))
            .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/affaires/" + affaireId + "/avenants/" + vide).with(user(SUB)))
            .andExpect(status().isNoContent());

        String avenant = creerAvenant("Parquet chambre");
        send(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/lignes"), ligne("Parquet", "12", "55"))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/envoyer").with(user(SUB)))
            .andExpect(status().isOk());
        send(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/decision"), """
            {"decision": "REFUSE", "par": "Mme Roux", "commentaire": "Trop cher"}
            """).andExpect(status().isOk());
        mockMvc.perform(delete("/api/affaires/" + affaireId + "/avenants/" + avenant).with(user(SUB)))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/marche").with(user(SUB)))
            .andExpect(jsonPath("$.actuelHt").value(1800.00));

        mockMvc.perform(get("/api/affaires/" + affaireId + "/avenants").with(user("avenants-autre")))
            .andExpect(status().isNotFound());
    }
}
