package com.faro.visite;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
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

import com.faro.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class VisiteApiTest {

    private static final String SUB = "visite";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String affaireId;
    private String cuisine;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.clean(jdbc);
        affaireId = JsonPath.read(mockMvc.perform(post("/api/affaires").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"nom": "Maison Martin", "typeBien": "MAISON", "clientNom": "M. Martin"}
                    """))
            .andReturn().getResponse().getContentAsString(), "$.id");
        cuisine = JsonPath.read(mockMvc.perform(post("/api/affaires/" + affaireId + "/pieces").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"nom": "Cuisine", "surfaceM2": 12, "perimetreMl": 14, "hauteurM": 2.5}
                    """))
            .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().setColor(Color.ORANGE);
        image.getGraphics().fillRect(0, 0, 40, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void recordsObservationsAndPhotosAndExportsTheReport() throws Exception {
        mockMvc.perform(post("/api/affaires/" + affaireId + "/observations").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"texte": "Accès par la cour, pas d'ascenseur"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.auteur").value(SUB));
        mockMvc.perform(post("/api/affaires/" + affaireId + "/observations").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"pieceId": "%s", "texte": "Évier à déplacer sous la fenêtre"}
                    """.formatted(cuisine)))
            .andExpect(status().isCreated());

        byte[] image = png();
        String photoId = JsonPath.read(mockMvc.perform(multipart("/api/affaires/" + affaireId + "/photos")
                .file(new MockMultipartFile("fichier", "cuisine.png", "image/png", image))
                .param("pieceId", cuisine).param("legende", "Mur de l'évier")
                .with(user(SUB)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taille").value(image.length))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/affaires/" + affaireId + "/visite").with(user(SUB)))
            .andExpect(jsonPath("$.observations", hasSize(2)))
            .andExpect(jsonPath("$.observations[1].pieceId").value(cuisine))
            .andExpect(jsonPath("$.photos", hasSize(1)))
            .andExpect(jsonPath("$.photos[0].legende").value("Mur de l'évier"))
            .andExpect(jsonPath("$.photos[0].pieceId").value(cuisine));

        mockMvc.perform(get("/api/affaires/" + affaireId + "/photos/" + photoId).with(user(SUB)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(image));

        byte[] pdf = mockMvc.perform(get("/api/affaires/" + affaireId + "/visite.pdf").with(user(SUB)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray();
        org.openpdf.text.pdf.PdfReader reader = new org.openpdf.text.pdf.PdfReader(pdf);
        String texte = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(texte).contains("Compte-rendu de visite — Maison Martin", "Généralités", "Accès par la cour",
            "Cuisine", "Sol : 12 m²", "murs : 35 m²", "Évier à déplacer", "Mur de l'évier");

        mockMvc.perform(delete("/api/affaires/" + affaireId + "/photos/" + photoId).with(user(SUB)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/visite").with(user(SUB)))
            .andExpect(jsonPath("$.photos", hasSize(0)));
    }

    @Test
    void rejectsFilesThatAreNotImages() throws Exception {
        mockMvc.perform(multipart("/api/affaires/" + affaireId + "/photos")
                .file(new MockMultipartFile("fichier", "virus.exe", "application/octet-stream", new byte[] {1, 2}))
                .with(user(SUB)))
            .andExpect(status().isUnsupportedMediaType());
    }

    private org.springframework.test.web.servlet.ResultActions envoyer(String nom, String type, byte[] donnees)
            throws Exception {
        return mockMvc.perform(multipart("/api/affaires/" + affaireId + "/photos")
            .file(new MockMultipartFile("fichier", nom, type, donnees)).with(user(SUB)));
    }

    private static byte[] image(int largeur, int hauteur, String format) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    @Test
    void checksTheRealContentOfPhotos() throws Exception {
        // Le type enregistré est celui détecté (alias image/jpg normalisé).
        envoyer("photo.jpg", "image/jpg", image(40, 30, "jpg")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.contentType").value("image/jpeg"));
        envoyer("photo.webp", "image/webp", VerificationImageTest.webpSansPerte(64, 48)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.contentType").value("image/webp"));

        // Signature incohérente avec le type annoncé, ou pas une image du tout.
        envoyer("photo.jpg", "image/jpeg", png()).andExpect(status().isUnsupportedMediaType());
        envoyer("page.png", "image/png", "<html><script>alert(1)</script></html>".getBytes())
            .andExpect(status().isUnsupportedMediaType());
        // Bonne signature mais contenu illisible (tronqué ou inventé).
        byte[] tronque = java.util.Arrays.copyOf(png(), 40);
        envoyer("tronque.png", "image/png", tronque).andExpect(status().isBadRequest());
        envoyer("faux.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5})
            .andExpect(status().isBadRequest());
        // Dimensions absurdes.
        envoyer("bande.png", "image/png", image(12_001, 1, "png")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Image trop grande (12001 × 1 pixels) : 12 000 pixels de côté au maximum"));
        envoyer("geante.webp", "image/webp", VerificationImageTest.webpSansPerte(16_000, 10)).andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/affaires/" + affaireId + "/visite").with(user(SUB)))
            .andExpect(jsonPath("$.photos", hasSize(2)));
    }

    @Test
    void anotherOrganisationCannotSeeTheVisit() throws Exception {
        String photoId = JsonPath.read(mockMvc.perform(multipart("/api/affaires/" + affaireId + "/photos")
                .file(new MockMultipartFile("fichier", "a.png", "image/png", png())).with(user(SUB)))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/affaires/" + affaireId + "/visite").with(user("visite-autre")))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/photos/" + photoId).with(user("visite-autre")))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/affaires/" + affaireId + "/observations").with(user("visite-autre"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"texte": "intrus"}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void deletingAPieceKeepsItsObservationsAsGeneral() throws Exception {
        mockMvc.perform(post("/api/affaires/" + affaireId + "/observations").with(user(SUB))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"pieceId": "%s", "texte": "Sol en tomettes"}
                    """.formatted(cuisine)))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/affaires/" + affaireId + "/pieces/" + cuisine).with(user(SUB)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/affaires/" + affaireId + "/visite").with(user(SUB)))
            .andExpect(jsonPath("$.observations[0].texte").value("Sol en tomettes"))
            .andExpect(jsonPath("$.observations[0].pieceId").doesNotExist());
    }
}
