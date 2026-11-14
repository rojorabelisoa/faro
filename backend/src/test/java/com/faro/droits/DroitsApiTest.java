package com.faro.droits;

import static com.faro.TestTokens.user;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.faro.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;

/** Matrice des droits par rôle d'entreprise (voir PROGRESS.md, « Décisions »). */
@SpringBootTest
@AutoConfigureMockMvc
class DroitsApiTest {

    private static final String GERANT = "droits-gerant";
    private static final String ETUDES = "droits-etudes";
    private static final String CONDUCTEUR = "droits-conducteur";

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
        mockMvc.perform(get("/api/me").with(user(GERANT))).andExpect(status().isOk());
        rejoindre(ETUDES, "CHARGE_ETUDES");
        rejoindre(CONDUCTEUR, "CONDUCTEUR_TRAVAUX");
        affaireId = id(send(post("/api/affaires"), GERANT, "{\"nom\": \"Maison Roux\", \"typeBien\": \"MAISON\"}")
            .andExpect(status().isCreated()));
        lotId = JsonPath.read(mockMvc.perform(get("/api/lots").with(user(GERANT))).andReturn().getResponse()
            .getContentAsString(), "$[7].id");
        ligneBase = id(send(post("/api/affaires/" + affaireId + "/lignes"), GERANT, ligne("Tableau électrique", "900"))
            .andExpect(status().isCreated()));
    }

    private void rejoindre(String sub, String role) throws Exception {
        String code = JsonPath.read(send(post("/api/entreprise/invitations"), GERANT, "{\"role\": \"" + role + "\"}")
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.code");
        mockMvc.perform(post("/api/invitations/" + code + "/accepter").with(user(sub))).andExpect(status().isNoContent());
    }

    private ResultActions send(MockHttpServletRequestBuilder requete, String sub, String json) throws Exception {
        return mockMvc.perform(requete.with(user(sub)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions lire(String url, String sub) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).with(user(sub)));
    }

    private static String id(ResultActions resultat) throws Exception {
        return JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String ligne(String designation, String prix) {
        return """
            {"lotId": "%s", "designation": "%s", "unite": "U", "quantite": 1, "prixUnitaireHt": %s,
             "debourseUnitaireHt": 500}
            """.formatted(lotId, designation, prix);
    }

    private void passerEnChantier() throws Exception {
        send(post("/api/affaires/" + affaireId + "/chantier"), GERANT, "{}").andExpect(status().isCreated());
    }

    @Test
    void theSiteManagerReadsTheJobButCannotChangeTheQuote() throws Exception {
        String a = "/api/affaires/" + affaireId;
        lire("/api/affaires", CONDUCTEUR).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        lire(a, CONDUCTEUR).andExpect(status().isOk());
        lire(a + "/pieces", CONDUCTEUR).andExpect(status().isOk());
        lire("/api/lots", CONDUCTEUR).andExpect(status().isOk());
        lire(a + "/visite", CONDUCTEUR).andExpect(status().isOk());
        lire(a + "/devis.pdf?vue=LOT", CONDUCTEUR).andExpect(status().isOk());
        lire(a + "/lots/" + lotId + "/bon-commande.pdf", CONDUCTEUR).andExpect(status().isOk());
        lire("/api/entreprise", CONDUCTEUR).andExpect(status().isOk());
        // Le devis est lisible, sans le déboursé sec (coût interne).
        lire(a + "/devis", CONDUCTEUR).andExpect(status().isOk())
            .andExpect(jsonPath("$.lignes[0].prixUnitaireHt").value(900.00))
            .andExpect(jsonPath("$.lignes[0].debourseUnitaireHt").doesNotExist());
        lire(a + "/devis", GERANT).andExpect(jsonPath("$.lignes[0].debourseUnitaireHt").value(500.00));

        send(post("/api/affaires"), CONDUCTEUR, "{\"nom\": \"Pirate\", \"typeBien\": \"MAISON\"}")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value(Droits.MESSAGE_CONDUCTEUR));
        send(put(a), CONDUCTEUR, "{\"nom\": \"Pirate\", \"typeBien\": \"MAISON\", \"statut\": \"PERDUE\"}")
            .andExpect(status().isForbidden());
        mockMvc.perform(delete(a).with(user(CONDUCTEUR))).andExpect(status().isForbidden());
        send(post(a + "/pieces"), CONDUCTEUR, "{\"nom\": \"Cuisine\"}").andExpect(status().isForbidden());
        send(post(a + "/lignes"), CONDUCTEUR, ligne("Prise", "80")).andExpect(status().isForbidden());
        send(put(a + "/lignes/" + ligneBase), CONDUCTEUR, ligne("Tableau", "1")).andExpect(status().isForbidden());
        mockMvc.perform(delete(a + "/lignes/" + ligneBase).with(user(CONDUCTEUR))).andExpect(status().isForbidden());
        send(put(a + "/gamme"), CONDUCTEUR, "{\"gamme\": \"PREMIUM\"}").andExpect(status().isForbidden());
        send(post(a + "/versions"), CONDUCTEUR, "{}").andExpect(status().isForbidden());
        send(post(a + "/chantier"), CONDUCTEUR, "{}").andExpect(status().isForbidden());
        lire("/api/ouvrages", CONDUCTEUR).andExpect(status().isForbidden());
        send(post("/api/ouvrages"), CONDUCTEUR, "{}").andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/ouvrages/import").file(new MockMultipartFile("fichier", "a.csv", "text/csv",
            new byte[] {1})).with(user(CONDUCTEUR))).andExpect(status().isForbidden());
        send(post("/api/lots"), CONDUCTEUR, "{\"code\": \"99\", \"nom\": \"Pirate\"}").andExpect(status().isForbidden());
        lire("/api/ratios", CONDUCTEUR).andExpect(status().isForbidden());
        lire(a + "/estimation", CONDUCTEUR).andExpect(status().isForbidden());
        lire(a + "/acces-client", CONDUCTEUR).andExpect(status().isForbidden());
        send(post(a + "/acces-client"), CONDUCTEUR, "{}").andExpect(status().isForbidden());
        lire(a + "/marge", CONDUCTEUR).andExpect(status().isForbidden());
        lire(a + "/bilan", CONDUCTEUR).andExpect(status().isForbidden());
        send(put("/api/entreprise"), CONDUCTEUR, "{\"nom\": \"Pirate\"}").andExpect(status().isForbidden());

        // Rien n'a bougé.
        lire(a + "/devis", GERANT).andExpect(jsonPath("$.lignes", hasSize(1)));
        lire("/api/affaires", GERANT).andExpect(jsonPath("$[0].nom").value("Maison Roux"));
    }

    @Test
    void theSiteManagerAddsVisitNotesAndPhotos() throws Exception {
        String a = "/api/affaires/" + affaireId;
        send(post(a + "/observations"), CONDUCTEUR, "{\"texte\": \"Fissure au plafond du séjour\"}")
            .andExpect(status().isCreated());
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        String photo = id(mockMvc.perform(multipart(a + "/photos")
                .file(new MockMultipartFile("fichier", "fissure.png", "image/png", png.toByteArray()))
                .with(user(CONDUCTEUR)))
            .andExpect(status().isCreated()));
        lire(a + "/photos/" + photo, CONDUCTEUR).andExpect(status().isOk());
        lire(a + "/visite", CONDUCTEUR)
            .andExpect(jsonPath("$.observations", hasSize(1)))
            .andExpect(jsonPath("$.photos", hasSize(1)));
        // Ajouter seulement : la suppression reste au gérant et au chargé d'études.
        mockMvc.perform(delete(a + "/photos/" + photo).with(user(CONDUCTEUR))).andExpect(status().isForbidden());
        mockMvc.perform(delete(a + "/photos/" + photo).with(user(ETUDES))).andExpect(status().isNoContent());
    }

    @Test
    void theSiteManagerHandlesAvenantsAndExpensesWithoutSeeingTheMargin() throws Exception {
        passerEnChantier();
        String a = "/api/affaires/" + affaireId;
        String avenant = id(send(post(a + "/avenants"), CONDUCTEUR, "{\"objet\": \"Prises supplémentaires\"}")
            .andExpect(status().isCreated()));
        send(put(a + "/avenants/" + avenant), CONDUCTEUR, "{\"objet\": \"Prises supplémentaires séjour\"}")
            .andExpect(status().isOk());
        String ligneAvenant = id(send(post(a + "/avenants/" + avenant + "/lignes"), CONDUCTEUR, ligne("Prise 16A", "85"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.debourseUnitaireHt").doesNotExist()));
        // Le déboursé envoyé par le conducteur est ignoré ; celui saisi au bureau est conservé.
        send(put(a + "/lignes/" + ligneAvenant), ETUDES, ligne("Prise 16A", "85")).andExpect(status().isOk());
        send(put(a + "/lignes/" + ligneAvenant), CONDUCTEUR, """
            {"lotId": "%s", "designation": "Prise 16A x2", "unite": "U", "quantite": 2, "prixUnitaireHt": 85}
            """.formatted(lotId)).andExpect(status().isOk());
        lire(a + "/avenants", GERANT)
            .andExpect(jsonPath("$[0].lignes[0].designation").value("Prise 16A x2"))
            .andExpect(jsonPath("$[0].lignes[0].debourseUnitaireHt").value(500.00));
        lire(a + "/avenants", CONDUCTEUR).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].lignes[0].debourseUnitaireHt").doesNotExist());
        lire(a + "/marche", CONDUCTEUR).andExpect(status().isOk());
        // Le devis de base reste figé pour lui comme pour les autres, et lui est interdit de toute façon.
        send(put(a + "/lignes/" + ligneBase), CONDUCTEUR, ligne("Tableau", "1")).andExpect(status().isForbidden());

        String depense = id(send(post(a + "/depenses"), CONDUCTEUR, """
            {"lotId": "%s", "nature": "MATERIAUX", "libelle": "Facture Cedeo", "montantHt": 120.50,
             "dateDepense": "2026-11-10"}
            """.formatted(lotId)).andExpect(status().isCreated()));
        send(put(a + "/depenses/" + depense), CONDUCTEUR, """
            {"lotId": "%s", "nature": "MATERIAUX", "libelle": "Facture Cedeo 1234", "montantHt": 130,
             "dateDepense": "2026-11-10"}
            """.formatted(lotId)).andExpect(status().isOk());
        lire(a + "/depenses", CONDUCTEUR).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].libelle").value("Facture Cedeo 1234"));
        lire(a + "/marge", CONDUCTEUR).andExpect(status().isForbidden());
        lire(a + "/marge", ETUDES).andExpect(status().isOk()).andExpect(jsonPath("$.total.coutReelHt").value(130.00));
    }

    @Test
    void theDashboardOfTheSiteManagerHasNoAmounts() throws Exception {
        lire("/api/tableau-de-bord", GERANT).andExpect(status().isOk())
            .andExpect(jsonPath("$.montants").value(true))
            .andExpect(jsonPath("$.signeCeMois.valeur").exists())
            .andExpect(jsonPath("$.signeParMois", hasSize(12)))
            .andExpect(jsonPath("$.pipeline[0].montantHt").value(900.00));
        lire("/api/tableau-de-bord", CONDUCTEUR).andExpect(status().isOk())
            .andExpect(jsonPath("$.montants").value(false))
            .andExpect(jsonPath("$.affairesActives.valeur").value(1))
            .andExpect(jsonPath("$.signeCeMois").doesNotExist())
            .andExpect(jsonPath("$.margeReelle").doesNotExist())
            .andExpect(jsonPath("$.enAttenteClient").doesNotExist())
            .andExpect(jsonPath("$.transformation").doesNotExist())
            .andExpect(jsonPath("$.signeParMois", hasSize(0)))
            .andExpect(jsonPath("$.pipeline[0].nombre").value(1))
            .andExpect(jsonPath("$.pipeline[0].montantHt").doesNotExist())
            .andExpect(jsonPath("$.aFaire").isArray());
    }

    @Test
    void theStudyManagerDoesEverythingButAdministerTheCompany() throws Exception {
        String a = "/api/affaires/" + affaireId;
        send(post(a + "/pieces"), ETUDES, "{\"nom\": \"Cuisine\"}").andExpect(status().isCreated());
        send(put(a + "/lignes/" + ligneBase), ETUDES, ligne("Tableau électrique", "950")).andExpect(status().isOk());
        lire(a + "/marge", ETUDES).andExpect(status().isOk());
        lire(a + "/bilan", ETUDES).andExpect(status().isOk());
        send(post(a + "/acces-client"), ETUDES, "{}").andExpect(status().isCreated());
        lire("/api/tableau-de-bord", ETUDES).andExpect(jsonPath("$.montants").value(true));
        send(put("/api/entreprise"), ETUDES, "{\"nom\": \"Pirate\"}").andExpect(status().isForbidden());
        send(post("/api/entreprise/invitations"), ETUDES, "{\"role\": \"GERANT\"}").andExpect(status().isForbidden());
    }
}
