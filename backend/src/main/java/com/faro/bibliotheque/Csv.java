package com.faro.bibliotheque;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Lecture / écriture CSV « Excel français » : séparateur « ; » (ou « , »), guillemets, UTF-8 ou Windows-1252. */
final class Csv {

    private Csv() {
    }

    /** Excel enregistre souvent en Windows-1252 : on essaie UTF-8 strict, sinon Windows-1252. */
    static String decode(byte[] contenu) {
        try {
            String texte = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(contenu)).toString();
            return texte.startsWith("﻿") ? texte.substring(1) : texte;
        } catch (CharacterCodingException e) {
            return new String(contenu, Charset.forName("windows-1252"));
        }
    }

    /** Lignes du fichier (lignes vides ignorées), découpées en champs. */
    static List<List<String>> parse(String texte) {
        String premiere = texte.lines().findFirst().orElse("");
        char separateur = premiere.indexOf(';') >= 0 || premiere.indexOf(',') < 0 ? ';' : ',';
        List<List<String>> lignes = new ArrayList<>();
        List<String> champs = new ArrayList<>();
        StringBuilder champ = new StringBuilder();
        boolean entreGuillemets = false;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (entreGuillemets) {
                if (c == '"' && i + 1 < texte.length() && texte.charAt(i + 1) == '"') {
                    champ.append('"');
                    i++;
                } else if (c == '"') {
                    entreGuillemets = false;
                } else {
                    champ.append(c);
                }
            } else if (c == '"') {
                entreGuillemets = true;
            } else if (c == separateur) {
                champs.add(champ.toString().trim());
                champ.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texte.length() && texte.charAt(i + 1) == '\n') {
                    i++;
                }
                champs.add(champ.toString().trim());
                champ.setLength(0);
                ajouterSiNonVide(lignes, champs);
                champs = new ArrayList<>();
            } else {
                champ.append(c);
            }
        }
        champs.add(champ.toString().trim());
        ajouterSiNonVide(lignes, champs);
        return lignes;
    }

    private static void ajouterSiNonVide(List<List<String>> lignes, List<String> champs) {
        if (champs.stream().anyMatch(champ -> !champ.isEmpty())) {
            lignes.add(champs);
        }
    }

    static String ligne(List<String> champs) {
        return String.join(";", champs.stream().map(Csv::echapper).toList()) + "\r\n";
    }

    private static String echapper(String champ) {
        if (champ == null) {
            return "";
        }
        return champ.contains(";") || champ.contains("\"") || champ.contains("\n")
            ? "\"" + champ.replace("\"", "\"\"") + "\"" : champ;
    }
}
