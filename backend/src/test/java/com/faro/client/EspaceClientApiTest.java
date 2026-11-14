package com.faro.client;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

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
class EspaceClientApiTest {

    private static final String SUB = "espace-client";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private String sdb;
    private String lot;
    private String code;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        mockMvc.perform(put("/api/entreprise").with(user(SUB)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\": \"Rénov Plus\"}"));
        affaireId = id(equipe("/api/affaires", "{\"nom\": \"T3 Croix-Rousse\", \"typeBien\": \"APPARTEMENT\"}"));
        sdb = id(equipe("/api/affaires/" + affaireId + "/pieces", "{\"nom\": \"Salle de bains\"}"));
        lot = JsonPath.read(mockMvc.perform(get("/api/lots").with(user(SUB))).andReturn().getResponse()
            .getContentAsString(), "$[9].id");
        for (String[] l : new String[][] {{"Dépose", "300", "null"}, {"Faïence simple", "500", "\"ECO\""},
                {"Faïence rectifiée", "800", "\"STANDARD\""}}) {
            equipe("/api/affaires/" + affaireId + "/lignes", """
                {"pieceId": "%s", "lotId": "%s", "designation": "%s", "unite": "U", "quantite": 1,
                 "prixUnitaireHt": %s, "gamme": %s, "debourseUnitaireHt": 100}
                """.formatted(sdb, lot, l[0], l[1], l[2]));
        }
        code = JsonPath.read(equipe("/api/affaires/" + affaireId + "/acces-client", ""), "$.code");
    }

    private String equipe(String url, String json) throws Exception {
        return mockMvc.perform(post(url).with(user(SUB)).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }

    @Test
    void theClientSeesTheDevisAndChoosesAGammeWithoutAnAccount() throws Exception {
        mockMvc.perform(get("/api/public/client/" + code))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entreprise").value("Rénov Plus"))
            .andExpect(jsonPath("$.affaire").value("T3 Croix-Rousse"))
            .andExpect(jsonPath("$.chantier").value(false))
            .andExpect(jsonPath("$.devis.totalHt").value(1100.00))
            .andExpect(jsonPath("$.devis.parPiece[0].gammes.ECO").value(800.00));

        mockMvc.perform(put("/api/public/client/" + code + "/pieces/" + sdb + "/gamme")
                .contentType(MediaType.APPLICATION_JSON).content("{\"gamme\": \"ECO\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/public/client/" + code))
            .andExpect(jsonPath("$.devis.totalHt").value(800.00));
        mockMvc.perform(get("/api/public/client/" + code + "/devis.pdf"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/api/affaires/" + affaireId + "/acces-client").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].derniereVisite").isNotEmpty());
    }

    @Test
    void theClientValidatesASentAvenantButNotADraft() throws Exception {
        equipe("/api/affaires/" + affaireId + "/chantier", "{}");
        mockMvc.perform(put("/api/public/client/" + code + "/pieces/" + sdb + "/gamme")
                .contentType(MediaType.APPLICATION_JSON).content("{\"gamme\": \"PREMIUM\"}"))
            .andExpect(status().isConflict());

        String brouillon = id(equipe("/api/affaires/" + affaireId + "/avenants", "{\"objet\": \"Brouillon\"}"));
        String avenant = id(equipe("/api/affaires/" + affaireId + "/avenants", "{\"objet\": \"Sèche-serviettes\"}"));
        equipe("/api/affaires/" + affaireId + "/avenants/" + avenant + "/lignes", """
            {"lotId": "%s", "designation": "Sèche-serviettes", "unite": "U", "quantite": 1, "prixUnitaireHt": 640}
            """.formatted(lot));
        mockMvc.perform(post("/api/affaires/" + affaireId + "/avenants/" + avenant + "/envoyer").with(user(SUB)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/client/" + code))
            .andExpect(jsonPath("$.chantier").value(true))
            .andExpect(jsonPath("$.avenants", hasSize(1)))
            .andExpect(jsonPath("$.marche.avenantsEnAttenteHt").value(640.00));
        mockMvc.perform(post("/api/public/client/" + code + "/avenants/" + brouillon + "/decision")
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\": \"VALIDE\", \"nom\": \"Mme Roux\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/public/client/" + code + "/avenants/" + avenant + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\": \"VALIDE\", \"nom\": \"Mme Roux\", \"commentaire\": \"D'accord\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/avenants").with(user(SUB)))
            .andExpect(jsonPath("$[1].statut").value("VALIDE"))
            .andExpect(jsonPath("$[1].decidePar").value("Mme Roux (espace client)"));
        mockMvc.perform(get("/api/public/client/" + code + "/avenants/" + avenant + "/document.pdf"))
            .andExpect(status().isOk());
    }

    @Test
    void aRevokedOrUnknownLinkGivesNothingAndTheRestOfTheApiStaysProtected() throws Exception {
        String lien = JsonPath.read(mockMvc.perform(get("/api/affaires/" + affaireId + "/acces-client").with(user(SUB)))
            .andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(delete("/api/affaires/" + affaireId + "/acces-client/" + lien).with(user(SUB)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/public/client/" + code)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/client/inconnu")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/affaires")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/affaires/" + affaireId + "/acces-client").with(user("espace-autre")))
            .andExpect(status().isNotFound());
    }

    @Test
    void theClientNeverSeesTheCompanyCosts() throws Exception {
        mockMvc.perform(get("/api/public/client/" + code))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.devis.lignes[0].designation").exists())
            .andExpect(jsonPath("$.devis.lignes[0].debourseUnitaireHt").doesNotExist());
    }

    @Test
    void aLinkExpiresAfter90DaysAndCanBeExtended() throws Exception {
        String liens = mockMvc.perform(get("/api/affaires/" + affaireId + "/acces-client").with(user(SUB)))
            .andExpect(jsonPath("$[0].expire").value(false))
            .andReturn().getResponse().getContentAsString();
        String lien = JsonPath.read(liens, "$[0].id");
        Instant expiration = Instant.parse(JsonPath.read(liens, "$[0].expireAt"));
        assertThat(Duration.between(Instant.now(), expiration).toDays()).isBetween(89L, 90L);

        // Expiré : le client reçoit la même réponse que pour un lien inconnu, partout.
        jdbc.update("UPDATE acces_client SET expire_at = now() - INTERVAL '1 minute'");
        String inconnu = mockMvc.perform(get("/api/public/client/inconnu")).andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/public/client/" + code)).andExpect(status().isNotFound())
            .andExpect(content().json(inconnu.replace("/api/public/client/inconnu", "/api/public/client/" + code)));
        mockMvc.perform(get("/api/public/client/" + code + "/devis.pdf")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/public/client/" + code + "/gamme")
                .contentType(MediaType.APPLICATION_JSON).content("{\"gamme\": \"ECO\"}"))
            .andExpect(status().isNotFound());

        // L'entreprise le voit toujours, expiré, et le prolonge de 90 jours à partir d'aujourd'hui.
        mockMvc.perform(get("/api/affaires/" + affaireId + "/acces-client").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].expire").value(true));
        mockMvc.perform(post("/api/affaires/" + affaireId + "/acces-client/" + lien + "/prolonger").with(user("espace-autre")))
            .andExpect(status().isNotFound());
        String prolonge = mockMvc.perform(post("/api/affaires/" + affaireId + "/acces-client/" + lien + "/prolonger")
                .with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expire").value(false))
            .andReturn().getResponse().getContentAsString();
        assertThat(Duration.between(Instant.now(), Instant.parse(JsonPath.read(prolonge, "$.expireAt"))).toDays())
            .isBetween(89L, 90L);
        mockMvc.perform(get("/api/public/client/" + code)).andExpect(status().isOk());

        // Un lien révoqué ne se prolonge pas.
        mockMvc.perform(delete("/api/affaires/" + affaireId + "/acces-client/" + lien).with(user(SUB)))
            .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/affaires/" + affaireId + "/acces-client/" + lien + "/prolonger").with(user(SUB)))
            .andExpect(status().isNotFound());
    }
}
