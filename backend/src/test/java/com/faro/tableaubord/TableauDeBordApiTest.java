package com.faro.tableaubord;

import static com.faro.TestTokens.user;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class TableauDeBordApiTest {

    private static final String SUB = "tableau";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String lot;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        lot = JsonPath.read(mockMvc.perform(get("/api/lots").with(user(SUB))).andReturn().getResponse()
            .getContentAsString(), "$[2].id");
    }

    private String post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .with(user(SUB)).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();
    }

    private String affaire(String nom) throws Exception {
        return JsonPath.read(post("/api/affaires", "{\"nom\": \"" + nom + "\", \"typeBien\": \"MAISON\"}"), "$.id");
    }

    private void ligne(String url, String montant) throws Exception {
        post(url, """
            {"lotId": "%s", "designation": "Travaux", "unite": "forfait", "quantite": 1, "prixUnitaireHt": %s}
            """.formatted(lot, montant));
    }

    private void statut(String id, String nom, String statut) throws Exception {
        mockMvc.perform(put("/api/affaires/" + id).with(user(SUB)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\": \"" + nom + "\", \"typeBien\": \"MAISON\", \"statut\": \"" + statut + "\"}"));
    }

    @Test
    void summarisesTheCompanyActivity() throws Exception {
        String gagnee = affaire("Maison gagnée");
        ligne("/api/affaires/" + gagnee + "/lignes", "10000");
        post("/api/affaires/" + gagnee + "/chantier", "{}");
        String valide = JsonPath.read(post("/api/affaires/" + gagnee + "/avenants", "{\"objet\": \"Prises\"}"), "$.id");
        ligne("/api/affaires/" + gagnee + "/avenants/" + valide + "/lignes", "500");
        post("/api/affaires/" + gagnee + "/avenants/" + valide + "/envoyer", "");
        post("/api/affaires/" + gagnee + "/avenants/" + valide + "/decision", "{\"decision\": \"VALIDE\", \"par\": \"Client\"}");
        String attente = JsonPath.read(post("/api/affaires/" + gagnee + "/avenants", "{\"objet\": \"Spots\"}"), "$.id");
        ligne("/api/affaires/" + gagnee + "/avenants/" + attente + "/lignes", "300");
        post("/api/affaires/" + gagnee + "/avenants/" + attente + "/envoyer", "");
        post("/api/affaires/" + gagnee + "/depenses", """
            {"lotId": "%s", "nature": "MATERIAUX", "libelle": "Matériaux", "montantHt": 6000}
            """.formatted(lot));
        String code = JsonPath.read(post("/api/affaires/" + gagnee + "/acces-client", ""), "$.code");
        mockMvc.perform(get("/api/public/client/" + code)).andExpect(status().isOk());

        String estimation = affaire("Appartement à relancer");
        ligne("/api/affaires/" + estimation + "/lignes", "2000");
        statut(estimation, "Appartement à relancer", "ESTIMATION");
        jdbc.update("UPDATE affaire SET updated_at = now() - interval '20 days' WHERE id = ?::uuid", estimation);
        statut(affaire("Maison perdue"), "Maison perdue", "PERDUE");
        affaire("Studio en visite");

        mockMvc.perform(get("/api/tableau-de-bord").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affairesActives.valeur").value(3))
            .andExpect(jsonPath("$.affairesActives.serie", hasSize(6)))
            .andExpect(jsonPath("$.affairesActives.serie[5]").value(4))
            .andExpect(jsonPath("$.signeCeMois.valeur").value(10500.00))
            .andExpect(jsonPath("$.signeCeMois.tendance").doesNotExist())
            .andExpect(jsonPath("$.margeReelle.valeur").value(42.9))
            .andExpect(jsonPath("$.enAttenteClient.valeur").value(300.00))
            .andExpect(jsonPath("$.transformation.taux").value(50.0))
            .andExpect(jsonPath("$.transformation.gagnees").value(1))
            .andExpect(jsonPath("$.transformation.perdues").value(1))
            .andExpect(jsonPath("$.transformation.enCours").value(2))
            .andExpect(jsonPath("$.signeParMois", hasSize(12)))
            .andExpect(jsonPath("$.signeParMois[11].montantHt").value(10500.00))
            .andExpect(jsonPath("$.pipeline[0].statut").value("VISITE"))
            .andExpect(jsonPath("$.pipeline[0].nombre").value(1))
            .andExpect(jsonPath("$.pipeline[1].montantHt").value(2000.00))
            .andExpect(jsonPath("$.pipeline[3].statut").value("CHANTIER"))
            .andExpect(jsonPath("$.pipeline[3].montantHt").value(10500.00))
            .andExpect(jsonPath("$.aFaire[*].type", hasItems("AVENANT_EN_ATTENTE", "A_RELANCER", "CLIENT_A_CONSULTE")));
    }

    @Test
    void anEmptyCompanyHasNeutralIndicators() throws Exception {
        mockMvc.perform(get("/api/tableau-de-bord").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affairesActives.valeur").value(0))
            .andExpect(jsonPath("$.margeReelle.valeur").doesNotExist())
            .andExpect(jsonPath("$.transformation.taux").doesNotExist())
            .andExpect(jsonPath("$.aFaire", hasSize(0)));
    }
}
