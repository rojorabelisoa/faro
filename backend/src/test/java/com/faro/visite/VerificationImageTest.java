package com.faro.visite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class VerificationImageTest {

    /** En-tête d'un WebP sans perte (VP8L) aux dimensions données ; le reste du flux n'est pas vérifié. */
    static byte[] webpSansPerte(int largeur, int hauteur) {
        int bits = (largeur - 1) & 0x3FFF | ((hauteur - 1) & 0x3FFF) << 14;
        return webp("VP8L", ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
            .put((byte) 0x2F).putInt(bits).put(new byte[5]).array());
    }

    static byte[] webp(String bloc, byte[] contenu) {
        ByteBuffer buffer = ByteBuffer.allocate(20 + contenu.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes()).putInt(12 + contenu.length).put("WEBP".getBytes())
            .put(bloc.getBytes()).putInt(contenu.length).put(contenu);
        return buffer.array();
    }

    @Test
    void readsTheDimensionsOfTheThreeWebpVariants() {
        assertThat(VerificationImage.dimensionsWebp(webpSansPerte(1600, 1200))).containsExactly(1600, 1200);
        byte[] avecPerte = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
            .put(new byte[] {0, 0, 0, (byte) 0x9D, 0x01, 0x2A}).putShort((short) 800).putShort((short) 600).array();
        assertThat(VerificationImage.dimensionsWebp(webp("VP8 ", avecPerte))).containsExactly(800, 600);
        byte[] etendu = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).put(new byte[4])
            .put(new byte[] {(byte) 0x3F, 0x06, 0}).put(new byte[] {(byte) 0xAF, 0x04, 0}).array();
        assertThat(VerificationImage.dimensionsWebp(webp("VP8X", etendu))).containsExactly(1600, 1200);
        // Bloc inconnu ou taille RIFF plus grande que le fichier.
        assertThat(VerificationImage.dimensionsWebp(webp("ABCD", new byte[10]))).isNull();
        byte[] menteur = webpSansPerte(10, 10);
        menteur[4] = 0x7F;
        assertThat(VerificationImage.dimensionsWebp(menteur)).isNull();
    }

    @Test
    void rejectsARiffFileThatIsNotWebp() {
        byte[] wav = webp("fmt ", new byte[10]);
        System.arraycopy("WAVE".getBytes(), 0, wav, 8, 4);
        assertThatThrownBy(() -> VerificationImage.verifier("image/webp", wav))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(415));
        assertThatThrownBy(() -> VerificationImage.verifier("image/webp", webp("ABCD", new byte[10])))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void normalisesTheDeclaredType() {
        assertThat(VerificationImage.normaliser("IMAGE/JPEG; charset=binary")).isEqualTo("image/jpeg");
        assertThat(VerificationImage.normaliser("image/pjpeg")).isEqualTo("image/jpeg");
        assertThat(VerificationImage.normaliser(null)).isNull();
        assertThatThrownBy(() -> VerificationImage.verifier("image/gif", new ByteArrayOutputStream().toByteArray()))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(415));
    }
}
