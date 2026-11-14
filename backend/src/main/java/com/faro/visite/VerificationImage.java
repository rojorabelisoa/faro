package com.faro.visite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Vérifie le contenu réel d'une photo de visite, sans croire le type annoncé par le navigateur :
 * signature du fichier (JPEG, PNG, WebP) cohérente avec le type déclaré, dimensions raisonnables, et décodage
 * complet par ImageIO pour JPEG et PNG. ImageIO ne lit pas le WebP : on contrôle alors l'en-tête RIFF/WEBP et
 * les dimensions écrites dans le premier bloc (VP8, VP8L ou VP8X), sans dépendance supplémentaire.
 */
final class VerificationImage {

    static final String JPEG = "image/jpeg";
    static final String PNG = "image/png";
    static final String WEBP = "image/webp";

    /** Au-delà, l'image est refusée (une photo réduite par le navigateur fait 1 600 px). */
    static final int COTE_MAX = 12_000;
    /** Le décodage de contrôle est réduit (sous-échantillonné) à environ cette taille : peu de mémoire. */
    private static final int COTE_CONTROLE = 1_024;

    private static final Map<String, String> ALIAS = Map.of(
        "image/jpg", JPEG, "image/pjpeg", JPEG, "image/x-png", PNG);

    private VerificationImage() {
    }

    /** Type réellement détecté (à enregistrer), ou exception 415 / 400 avec un message en français. */
    static String verifier(String typeDeclare, byte[] donnees) {
        String declare = normaliser(typeDeclare);
        if (declare == null || !VisiteService.TYPES_IMAGE.contains(declare)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formats acceptés : JPEG, PNG, WebP");
        }
        if (donnees.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo vide");
        }
        String detecte = detecter(donnees);
        if (detecte == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Ce fichier n'est pas une image JPEG, PNG ou WebP");
        }
        if (!detecte.equals(declare)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Le contenu du fichier ne correspond pas à son type (" + declare + ")");
        }
        if (detecte.equals(WEBP)) {
            int[] dimensions = dimensionsWebp(donnees);
            if (dimensions == null) {
                throw illisible();
            }
            verifierDimensions(dimensions[0], dimensions[1]);
        } else {
            decoder(donnees, detecte.equals(JPEG) ? "jpeg" : "png");
        }
        return detecte;
    }

    static String normaliser(String type) {
        if (type == null) {
            return null;
        }
        String simple = type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return ALIAS.getOrDefault(simple, simple);
    }

    /** Type d'après les premiers octets (signature), null si aucun des trois. */
    static String detecter(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return PNG;
        }
        if (d.length >= 16 && ascii(d, 0, "RIFF") && ascii(d, 8, "WEBP")) {
            return WEBP;
        }
        return null;
    }

    /** Lit les dimensions sans décoder l'image, les vérifie, puis décode toute l'image (sous-échantillonnée). */
    private static void decoder(byte[] donnees, String format) {
        Iterator<ImageReader> lecteurs = ImageIO.getImageReadersByFormatName(format);
        if (!lecteurs.hasNext()) {
            throw new IllegalStateException("Aucun lecteur ImageIO pour " + format);
        }
        ImageReader lecteur = lecteurs.next();
        try (ImageInputStream flux = new MemoryCacheImageInputStream(new ByteArrayInputStream(donnees))) {
            lecteur.setInput(flux, true, true);
            int largeur = lecteur.getWidth(0);
            int hauteur = lecteur.getHeight(0);
            verifierDimensions(largeur, hauteur);
            ImageReadParam parametres = lecteur.getDefaultReadParam();
            int pas = Math.max(1, Math.max(largeur, hauteur) / COTE_CONTROLE);
            parametres.setSourceSubsampling(pas, pas, 0, 0);
            if (lecteur.read(0, parametres) == null) {
                throw illisible();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ResponseStatusException erreur) {
                throw erreur;
            }
            throw illisible();
        } finally {
            lecteur.dispose();
        }
    }

    /**
     * Dimensions d'un WebP d'après son premier bloc, null si l'en-tête est incohérent.
     * VP8 (avec perte) : code de début 9D 01 2A puis largeur et hauteur sur 14 bits ;
     * VP8L (sans perte) : octet 0x2F puis largeur − 1 et hauteur − 1 sur 14 bits ;
     * VP8X (étendu) : largeur − 1 et hauteur − 1 sur 24 bits.
     */
    static int[] dimensionsWebp(byte[] d) {
        long tailleRiff = u32(d, 4);
        if (d.length < 30 || tailleRiff + 8 > d.length || tailleRiff < 22) {
            return null;
        }
        if (ascii(d, 12, "VP8 ")) {
            if ((d[23] & 0xFF) != 0x9D || (d[24] & 0xFF) != 0x01 || (d[25] & 0xFF) != 0x2A) {
                return null;
            }
            return new int[] {u16(d, 26) & 0x3FFF, u16(d, 28) & 0x3FFF};
        }
        if (ascii(d, 12, "VP8L")) {
            if ((d[20] & 0xFF) != 0x2F) {
                return null;
            }
            long bits = u32(d, 21);
            return new int[] {(int) (bits & 0x3FFF) + 1, (int) ((bits >> 14) & 0x3FFF) + 1};
        }
        if (ascii(d, 12, "VP8X")) {
            return new int[] {u24(d, 24) + 1, u24(d, 27) + 1};
        }
        return null;
    }

    private static void verifierDimensions(int largeur, int hauteur) {
        if (largeur <= 0 || hauteur <= 0) {
            throw illisible();
        }
        if (largeur > COTE_MAX || hauteur > COTE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Image trop grande (" + largeur + " × " + hauteur + " pixels) : 12 000 pixels de côté au maximum");
        }
    }

    private static ResponseStatusException illisible() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image illisible ou endommagée");
    }

    private static boolean ascii(byte[] d, int debut, String texte) {
        if (d.length < debut + texte.length()) {
            return false;
        }
        for (int i = 0; i < texte.length(); i++) {
            if (d[debut + i] != texte.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int u16(byte[] d, int i) {
        return (d[i] & 0xFF) | (d[i + 1] & 0xFF) << 8;
    }

    private static int u24(byte[] d, int i) {
        return u16(d, i) | (d[i + 2] & 0xFF) << 16;
    }

    private static long u32(byte[] d, int i) {
        return (u24(d, i) | (long) (d[i + 3] & 0xFF) << 24);
    }
}
