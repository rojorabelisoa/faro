package com.faro.bibliotheque;

import static com.faro.TestTokens.user;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class OuvrageApiTest {

    private static final String SUB = "bibliotheque";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String plomberie;
    private String carrelage;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        String lots = mockMvc.perform(get("/api/lots").with(user(SUB))).andReturn().getResponse().getContentAsString();
        plomberie = lotId(lots, "07");
        carrelage = lotId(lots, "10");
    }

    private static String lotId(String lotsJson, String code) {
        List<String> ids = JsonPath.read(lotsJson, "$[?(@.code == '" + code + "')].id");
        return ids.get(0);
    }

    private String createOuvrage(String lotId, String designation, String unite, String prix, String tva)
            throws Exception {
        String body = mockMvc.perform(post("/api/ouvrages").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"lotId": "%s", "designation": "%s", "unite": "%s", "prixUnitaireHt": %s, "tauxTva": %s}
                    """.formatted(lotId, designation, unite, prix, tva)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void searchesTheLibraryByWordAndByLot() throws Exception {
        createOuvrage(plomberie, "Fourniture et pose WC suspendu", "U", "690.00", "10");
        createOuvrage(carrelage, "Carrelage sol grès cérame 60x60", "m²", "72.40", "null");
        createOuvrage(carrelage, "Faïence murale 30x60", "m²", "65.00", "10");

        mockMvc.perform(get("/api/ouvrages").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].designation").value("Fourniture et pose WC suspendu"));
        mockMvc.perform(get("/api/ouvrages").param("q", "CARRELAGE").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tauxTva").value(10.0));
        mockMvc.perform(get("/api/ouvrages").param("lotId", carrelage).with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/ouvrages").with(user("bibliotheque-autre")))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aLineAddedFromTheLibraryCopiesTheOuvrage() throws Exception {
        String ouvrage = createOuvrage(carrelage, "Carrelage sol", "m²", "72.40", "10");
        String affaire = JsonPath.read(mockMvc.perform(post("/api/affaires").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"nom": "T2", "typeBien": "APPARTEMENT"}
                    """))
            .andReturn().getResponse().getContentAsString(), "$.id");
        String sdb = JsonPath.read(mockMvc.perform(post("/api/affaires/" + affaire + "/pieces").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"nom": "SDB"}
                    """))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/affaires/" + affaire + "/lignes/depuis-ouvrage").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ouvrageId": "%s", "pieceId": "%s", "quantite": 4.25}
                    """.formatted(ouvrage, sdb)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ouvrageId").value(ouvrage))
            .andExpect(jsonPath("$.lotId").value(carrelage))
            .andExpect(jsonPath("$.pieceId").value(sdb))
            .andExpect(jsonPath("$.designation").value("Carrelage sol"))
            .andExpect(jsonPath("$.montantHt").value(307.70));

        // Changer le prix de la bibliothèque ne modifie pas le devis existant.
        mockMvc.perform(put("/api/ouvrages/" + ouvrage).with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"lotId": "%s", "designation": "Carrelage sol", "unite": "m²", "prixUnitaireHt": 80}
                    """.formatted(carrelage)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/affaires/" + affaire + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.totalHt").value(307.70));

        // Supprimer l'ouvrage garde la ligne, sans lien.
        mockMvc.perform(delete("/api/ouvrages/" + ouvrage).with(user(SUB)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaire + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.lignes", hasSize(1)))
            .andExpect(jsonPath("$.lignes[0].ouvrageId").doesNotExist());
    }

    @Test
    void anotherOrganisationCannotUseTheOuvrage() throws Exception {
        String ouvrage = createOuvrage(plomberie, "WC", "U", "690", "10");
        String affaireAutre = JsonPath.read(mockMvc.perform(post("/api/affaires").with(user("bibliotheque-autre"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"nom": "X", "typeBien": "MAISON"}
                    """))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/affaires/" + affaireAutre + "/lignes/depuis-ouvrage").with(user("bibliotheque-autre"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ouvrageId": "%s", "quantite": 1}
                    """.formatted(ouvrage)))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/ouvrages/" + ouvrage).with(user("bibliotheque-autre")))
            .andExpect(status().isNotFound());
    }
}
