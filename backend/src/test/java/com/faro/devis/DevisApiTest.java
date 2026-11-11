package com.faro.devis;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;

import com.faro.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class DevisApiTest {

    private static final String SUB = "devis";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private String cuisine;
    private String sdb;
    private Map<String, String> lots;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        affaireId = id(postJson("/api/affaires", SUB, """
            {"nom": "T3 Lyon", "typeBien": "APPARTEMENT"}
            """));
        cuisine = id(postJson("/api/affaires/" + affaireId + "/pieces", SUB, """
            {"nom": "Cuisine"}
            """));
        sdb = id(postJson("/api/affaires/" + affaireId + "/pieces", SUB, """
            {"nom": "Salle de bains"}
            """));
        String lotsJson = mockMvc.perform(get("/api/lots").with(user(SUB))).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> list = JsonPath.read(lotsJson, "$");
        lots = new java.util.HashMap<>();
        list.forEach(lot -> lots.put((String) lot.get("code"), (String) lot.get("id")));
    }

    private String postJson(String url, String subject, String json) throws Exception {
        return mockMvc.perform(post(url).with(user(subject)).contentType(MediaType.APPLICATION_JSON).content(json))
            .andReturn().getResponse().getContentAsString();
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }

    private ResultActions addLigne(String pieceId, String lotCode, String designation, String unite,
            String quantite, String prix, String tva) throws Exception {
        String piece = pieceId == null ? "null" : "\"" + pieceId + "\"";
        String taux = tva == null ? "null" : tva;
        return mockMvc.perform(post("/api/affaires/" + affaireId + "/lignes").with(user(SUB))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"pieceId": %s, "lotId": "%s", "designation": "%s", "unite": "%s",
                 "quantite": %s, "prixUnitaireHt": %s, "tauxTva": %s}
                """.formatted(piece, lots.get(lotCode), designation, unite, quantite, prix, taux)));
    }

    @Test
    void devisIsTotalledByPieceByLotAndByVatRate() throws Exception {
        addLigne(null, "01", "Protection des sols", "forfait", "1", "350.00", null).andExpect(status().isCreated());
        addLigne(cuisine, "07", "Évier inox", "U", "1", "420.00", "10").andExpect(status().isCreated());
        addLigne(cuisine, "10", "Faïence murale", "m²", "8.5", "65.00", "10").andExpect(status().isCreated());
        addLigne(sdb, "10", "Carrelage sol", "m²", "4.25", "72.40", "10")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.montantHt").value(307.70));
        addLigne(sdb, "09", "VMC hygroréglable", "U", "1", "980.00", "5.5").andExpect(status().isCreated());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lignes", hasSize(5)))
            .andExpect(jsonPath("$.lignes[0].tauxTva").value(10.0))
            .andExpect(jsonPath("$.parPiece", hasSize(3)))
            .andExpect(jsonPath("$.parPiece[0].nom").value("Général"))
            .andExpect(jsonPath("$.parPiece[0].totalHt").value(350.00))
            .andExpect(jsonPath("$.parPiece[1].nom").value("Cuisine"))
            .andExpect(jsonPath("$.parPiece[1].totalHt").value(972.50))
            .andExpect(jsonPath("$.parPiece[2].totalHt").value(1287.70))
            .andExpect(jsonPath("$.parLot", hasSize(4)))
            .andExpect(jsonPath("$.parLot[0].code").value("01"))
            .andExpect(jsonPath("$.parLot[2].code").value("09"))
            .andExpect(jsonPath("$.parLot[3].code").value("10"))
            .andExpect(jsonPath("$.parLot[3].totalHt").value(860.20))
            .andExpect(jsonPath("$.tva", hasSize(2)))
            .andExpect(jsonPath("$.tva[0].taux").value(5.5))
            .andExpect(jsonPath("$.tva[0].montant").value(53.90))
            .andExpect(jsonPath("$.tva[1].baseHt").value(1630.20))
            .andExpect(jsonPath("$.tva[1].montant").value(163.02))
            .andExpect(jsonPath("$.totalHt").value(2610.20))
            .andExpect(jsonPath("$.totalTva").value(216.92))
            .andExpect(jsonPath("$.totalTtc").value(2827.12));
    }

    @Test
    void anEmptyDevisListsThePiecesWithZeroTotals() throws Exception {
        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parPiece", hasSize(2)))
            .andExpect(jsonPath("$.parPiece[0].totalHt").value(0))
            .andExpect(jsonPath("$.parLot", hasSize(0)))
            .andExpect(jsonPath("$.totalTtc").value(0));
    }

    @Test
    void updateMovesALineToAnotherPieceAndLot() throws Exception {
        String ligne = id(addLigne(cuisine, "07", "Évier", "U", "1", "420", null)
            .andReturn().getResponse().getContentAsString());

        mockMvc.perform(put("/api/affaires/" + affaireId + "/lignes/" + ligne).with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"pieceId": "%s", "lotId": "%s", "designation": "Vasque", "unite": "U",
                     "quantite": 2, "prixUnitaireHt": 180, "tauxTva": 20}
                    """.formatted(sdb, lots.get("07"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pieceId").value(sdb))
            .andExpect(jsonPath("$.montantHt").value(360.00));

        mockMvc.perform(delete("/api/affaires/" + affaireId + "/lignes/" + ligne).with(user(SUB)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.lignes", hasSize(0)));
    }

    @Test
    void linesCanBeReordered() throws Exception {
        String a = id(addLigne(cuisine, "07", "Évier", "U", "1", "420", null).andReturn().getResponse().getContentAsString());
        String b = id(addLigne(cuisine, "10", "Faïence", "m²", "2", "65", null).andReturn().getResponse().getContentAsString());
        String c = id(addLigne(sdb, "10", "Carrelage", "m²", "3", "70", null).andReturn().getResponse().getContentAsString());

        mockMvc.perform(put("/api/affaires/" + affaireId + "/lignes/ordre").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [\"%s\", \"%s\", \"%s\"]}".formatted(c, a, b)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.lignes[0].designation").value("Carrelage"))
            .andExpect(jsonPath("$.lignes[1].designation").value("Évier"))
            .andExpect(jsonPath("$.lignes[2].designation").value("Faïence"));

        mockMvc.perform(put("/api/affaires/" + affaireId + "/lignes/ordre").with(user("devis-autre"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [\"%s\"]}".formatted(a)))
            .andExpect(status().isNotFound());
    }

    @Test
    void devisIsExportedAsPdfByPieceAndByLot() throws Exception {
        addLigne(null, "01", "Protection des sols", "forfait", "1", "350.00", null).andExpect(status().isCreated());
        addLigne(sdb, "10", "Carrelage sol", "m²", "4.25", "72.40", "10").andExpect(status().isCreated());
        addLigne(sdb, "09", "VMC hygroréglable", "U", "1", "980.00", "5.5").andExpect(status().isCreated());

        byte[] parPiece = mockMvc.perform(get("/api/affaires/" + affaireId + "/devis.pdf").with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andReturn().getResponse().getContentAsByteArray();
        String texte = texte(parPiece);
        assertThat(texte).contains("Devis — T3 Lyon", "Général", "Salle de bains", "Carrelage sol", "VMC hygroréglable",
            "Total TTC", "1 757,37 €", "Bon pour accord");

        byte[] parLot = mockMvc.perform(get("/api/affaires/" + affaireId + "/devis.pdf").param("vue", "LOT")
                .with(user(SUB)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(texte(parLot)).contains("présentation par lot", "10 · Carrelage / Faïence", "1 757,37 €");

        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis.pdf").with(user("devis-autre")))
            .andExpect(status().isNotFound());
    }

    private static String texte(byte[] pdf) throws Exception {
        org.openpdf.text.pdf.PdfReader reader = new org.openpdf.text.pdf.PdfReader(pdf);
        org.openpdf.text.pdf.parser.PdfTextExtractor extracteur = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader);
        StringBuilder texte = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            texte.append(extracteur.getTextFromPage(page)).append('\n');
        }
        return texte.toString();
    }

    private ResultActions addLigneGamme(String pieceId, String designation, String prix, String gamme) throws Exception {
        String piece = pieceId == null ? "null" : "\"" + pieceId + "\"";
        return mockMvc.perform(post("/api/affaires/" + affaireId + "/lignes").with(user(SUB))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"pieceId\": %s, \"lotId\": \"%s\", \"designation\": \"%s\", \"unite\": \"U\", \"quantite\": 1, \"prixUnitaireHt\": %s, \"gamme\": %s}"
                .formatted(piece, lots.get("10"), designation, prix, gamme == null ? "null" : "\"" + gamme + "\"")));
    }

    @Test
    void onlyTheChosenGammeCountsAndEachGammeIsPriced() throws Exception {
        addLigneGamme(sdb, "Dépose", "300", null).andExpect(status().isCreated());
        addLigneGamme(sdb, "Carrelage 30x30", "500", "ECO").andExpect(status().isCreated());
        addLigneGamme(sdb, "Carrelage 60x60", "800", "STANDARD")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.incluse").value(true));
        addLigneGamme(sdb, "Grès cérame grand format", "1400", "PREMIUM")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.incluse").value(false));
        addLigneGamme(cuisine, "Crédence", "200", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.totalHt").value(1300.00))
            .andExpect(jsonPath("$.parPiece[1].gammeChoisie").value("STANDARD"))
            .andExpect(jsonPath("$.parPiece[1].gammes.ECO").value(800.00))
            .andExpect(jsonPath("$.parPiece[1].gammes.STANDARD").value(1100.00))
            .andExpect(jsonPath("$.parPiece[1].gammes.PREMIUM").value(1700.00))
            .andExpect(jsonPath("$.parPiece[0].gammes").doesNotExist())
            .andExpect(jsonPath("$.totauxParGamme.PREMIUM").value(1900.00))
            .andExpect(jsonPath("$.lignes[3].incluse").value(false));

        mockMvc.perform(put("/api/affaires/" + affaireId + "/pieces/" + sdb + "/gamme").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("{\"gamme\": \"PREMIUM\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.totalHt").value(1900.00))
            .andExpect(jsonPath("$.parLot[0].totalHt").value(1900.00))
            .andExpect(jsonPath("$.lignes[3].incluse").value(true))
            .andExpect(jsonPath("$.lignes[2].incluse").value(false));

        String texte = texte(mockMvc.perform(get("/api/affaires/" + affaireId + "/devis.pdf").with(user(SUB)))
            .andReturn().getResponse().getContentAsByteArray());
        assertThat(texte).contains("Grès cérame grand format").doesNotContain("Carrelage 60x60");

        // Lignes générales : gamme choisie au niveau de l'affaire.
        addLigneGamme(null, "Protection Premium", "100", "PREMIUM").andExpect(status().isCreated());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.totalHt").value(1900.00));
        mockMvc.perform(put("/api/affaires/" + affaireId + "/gamme").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("{\"gamme\": \"PREMIUM\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user(SUB)))
            .andExpect(jsonPath("$.totalHt").value(2000.00))
            .andExpect(jsonPath("$.parPiece[0].gammeChoisie").value("PREMIUM"));

        mockMvc.perform(put("/api/affaires/" + affaireId + "/pieces/" + sdb + "/gamme").with(user("devis-autre"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"gamme\": \"ECO\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void devisIsExportedAsAnExcelCsvWithoutUnchosenOptions() throws Exception {
        addLigne(null, "01", "Protection; sols", "forfait", "1", "350.00", null).andExpect(status().isCreated());
        addLigne(sdb, "10", "Carrelage sol", "m²", "4.25", "72.40", "10").andExpect(status().isCreated());
        mockMvc.perform(post("/api/affaires/" + affaireId + "/lignes").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pieceId\": \"%s\", \"lotId\": \"%s\", \"designation\": \"Zellige\", \"unite\": \"m²\", \"quantite\": 2, \"prixUnitaireHt\": 300, \"gamme\": \"PREMIUM\"}"
                    .formatted(sdb, lots.get("10"))))
            .andExpect(status().isCreated());

        String csv = new String(mockMvc.perform(get("/api/affaires/" + affaireId + "/devis.csv").with(user(SUB)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("lot_code;lot;piece;designation;description;unite;quantite;prix_unitaire_ht;tva;montant_ht;gamme",
            "01;Installation de chantier / Protections;Général;\"Protection; sols\";;forfait;1;350;10;350;",
            "10;Carrelage / Faïence;Salle de bains;Carrelage sol;;m²;4,25;72,4;10;307,7;")
            .doesNotContain("Zellige");
    }

    @Test
    void rejectsInvalidLines() throws Exception {
        addLigne(cuisine, "07", "Évier", "U", "1", "420", "7").andExpect(status().isBadRequest());
        addLigne(cuisine, "07", "Évier", "U", "-1", "420", null).andExpect(status().isBadRequest());
        addLigne(cuisine, "07", "", "U", "1", "420", null).andExpect(status().isBadRequest());
    }

    @Test
    void aLineCannotUseAPieceOfAnotherAffaireOrALotOfAnotherOrganisation() throws Exception {
        String autreAffaire = id(postJson("/api/affaires", SUB, """
            {"nom": "Autre", "typeBien": "MAISON"}
            """));
        String pieceAutreAffaire = id(postJson("/api/affaires/" + autreAffaire + "/pieces", SUB, """
            {"nom": "Garage"}
            """));
        addLigne(pieceAutreAffaire, "07", "Évier", "U", "1", "420", null).andExpect(status().isNotFound());

        String lotsAutreOrg = mockMvc.perform(get("/api/lots").with(user("devis-autre")))
            .andReturn().getResponse().getContentAsString();
        String lotAutreOrg = JsonPath.read(lotsAutreOrg, "$[0].id");
        mockMvc.perform(post("/api/affaires/" + affaireId + "/lignes").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"lotId": "%s", "designation": "X", "unite": "U", "quantite": 1, "prixUnitaireHt": 1}
                    """.formatted(lotAutreOrg)))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/devis").with(user("devis-autre")))
            .andExpect(status().isNotFound());
    }

    @Test
    void aPieceOrALotUsedByALineCannotBeDeleted() throws Exception {
        addLigne(cuisine, "07", "Évier", "U", "1", "420", null).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/affaires/" + affaireId + "/pieces/" + cuisine).with(user(SUB)))
            .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/lots/" + lots.get("07")).with(user(SUB)))
            .andExpect(status().isConflict());
    }

    @Test
    void deletingTheAffaireDeletesItsLines() throws Exception {
        addLigne(cuisine, "07", "Évier", "U", "1", "420", null).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/affaires/" + affaireId).with(user(SUB)))
            .andExpect(status().isNoContent());
    }
}
