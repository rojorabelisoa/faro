package com.faro.bibliotheque;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.faro.DatabaseCleaner;

@SpringBootTest
@AutoConfigureMockMvc
class OuvrageCsvApiTest {

    private static final String SUB = "csv";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    private ResultActions importer(byte[] contenu) throws Exception {
        return mockMvc.perform(multipart("/api/ouvrages/import")
            .file(new MockMultipartFile("fichier", "bibliotheque.csv", "text/csv", contenu))
            .with(user(SUB)));
    }

    @Test
    void importsAnExcelFrenchCsvInWindows1252() throws Exception {
        String csv = """
            Lot;Désignation;Unité;Prix unitaire HT;TVA;Descriptif
            07;Fourniture et pose WC suspendu;U;690,00;10;
            10;"Carrelage sol 60x60; grès cérame";m²;72,40 €;10 %;Pose droite
            08;Prise RJ45;U;1 095,5;;

            """;
        importer(csv.getBytes(Charset.forName("windows-1252")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.importes").value(3));

        mockMvc.perform(get("/api/ouvrages").param("q", "grès").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].designation").value("Carrelage sol 60x60; grès cérame"))
            .andExpect(jsonPath("$[0].prixUnitaireHt").value(72.40))
            .andExpect(jsonPath("$[0].unite").value("m²"))
            .andExpect(jsonPath("$[0].description").value("Pose droite"));
        mockMvc.perform(get("/api/ouvrages").param("q", "RJ45").with(user(SUB)))
            .andExpect(jsonPath("$[0].prixUnitaireHt").value(1095.50))
            .andExpect(jsonPath("$[0].tauxTva").value(10.0));
    }

    @Test
    void importsNothingAndListsErrorsWhenALineIsInvalid() throws Exception {
        String csv = """
            lot;designation;unite;prix_unitaire_ht;tva
            07;WC;U;690;10
            99;Inconnu;U;10;10
            07;Lavabo;U;abc;10
            07;Douche;U;900;7
            """;
        importer(csv.getBytes(StandardCharsets.UTF_8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.importes").value(0))
            .andExpect(jsonPath("$.erreurs", hasSize(3)))
            .andExpect(jsonPath("$.erreurs[0].ligne").value(3))
            .andExpect(jsonPath("$.erreurs[1].message").value("prix unitaire HT invalide « abc »"))
            .andExpect(jsonPath("$.erreurs[2].ligne").value(5));

        mockMvc.perform(get("/api/ouvrages").with(user(SUB))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void reimportingUpdatesPricesInsteadOfDuplicating() throws Exception {
        importer("lot;designation;unite;prix_unitaire_ht\n07;WC suspendu;U;690\n".getBytes(StandardCharsets.UTF_8))
            .andExpect(status().isOk());
        importer("lot;designation;unite;prix_unitaire_ht\n07;wc suspendu;U;720\n08;Prise;U;60\n"
                .getBytes(StandardCharsets.UTF_8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.importes").value(1))
            .andExpect(jsonPath("$.misAJour").value(1));

        mockMvc.perform(get("/api/ouvrages").with(user(SUB)))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].prixUnitaireHt").value(720.00));

        // Une erreur annule aussi les mises à jour.
        importer("lot;designation;unite;prix_unitaire_ht\n07;WC suspendu;U;999\n99;X;U;1\n"
                .getBytes(StandardCharsets.UTF_8))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/ouvrages").param("q", "WC").with(user(SUB)))
            .andExpect(jsonPath("$[0].prixUnitaireHt").value(720.00));
    }

    @Test
    void rejectsAFileWithoutTheRequiredColumns() throws Exception {
        importer("nom;prix\nWC;690\n".getBytes(StandardCharsets.UTF_8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erreurs[0].message").value(org.hamcrest.Matchers.startsWith("Colonnes manquantes")));
    }

    @Test
    void exportCanBeReimported() throws Exception {
        importer("""
            lot;designation;unite;prix_unitaire_ht;tva;description
            07;"WC \"\"Geberit\"\"";U;690;5,5;"Avec bâti-support; plaque blanche"
            """.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());

        byte[] export = mockMvc.perform(get("/api/ouvrages/export.csv").with(user(SUB)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray();
        String texte = new String(export, StandardCharsets.UTF_8);
        assertThat(texte).contains("lot;designation;unite;prix_unitaire_ht;tva;description",
            "07;\"WC \"\"Geberit\"\"\";U;690;5,5;\"Avec bâti-support; plaque blanche\"");

        DatabaseCleaner.clean(jdbc);
        importer(export).andExpect(status().isOk()).andExpect(jsonPath("$.importes").value(1));
        mockMvc.perform(get("/api/ouvrages").with(user(SUB)))
            .andExpect(jsonPath("$[0].designation").value("WC \"Geberit\""))
            .andExpect(jsonPath("$[0].tauxTva").value(5.5));
    }
}
