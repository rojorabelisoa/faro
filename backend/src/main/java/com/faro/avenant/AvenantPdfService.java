package com.faro.avenant;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import com.faro.common.Totaux;
import com.faro.devis.DevisResponse.LigneResponse;

/** Avenant en PDF : contenu exact envoyé au client et, le cas échéant, sa décision. */
@Service
public class AvenantPdfService {

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font DISCRET = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    private static final float[] LARGEURS = {32, 13, 15, 7, 7, 10, 6, 10};

    public byte[] pdf(AvenantDocument document) {
        AvenantResponse avenant = document.avenant();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(pdf, out);
        pdf.open();
        pdf.add(new Paragraph(document.entreprise(), GRAS));
        Paragraph titre = new Paragraph("Avenant n° " + avenant.numero() + " — " + avenant.objet(), TITRE);
        titre.setSpacingBefore(8);
        pdf.add(titre);
        pdf.add(new Paragraph("Affaire : " + document.affaire()
            + (document.client() != null ? " · " + document.client() : "")
            + (document.adresse() != null ? " · " + document.adresse() : ""), NORMAL));
        pdf.add(new Paragraph("Envoyé le " + date(avenant.envoyeAt()), DISCRET));
        if (avenant.motif() != null) {
            Paragraph motif = new Paragraph("Motif : " + avenant.motif(), NORMAL);
            motif.setSpacingBefore(8);
            pdf.add(motif);
        }

        PdfPTable table = new PdfPTable(LARGEURS.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);
        table.setWidths(LARGEURS);
        table.setHeaderRows(1);
        for (String entete : List.of("Désignation", "Pièce", "Lot", "Qté", "Unité", "PU HT", "TVA", "Total HT")) {
            table.addCell(cellule(entete, GRAS, List.of("Désignation", "Pièce", "Lot", "Unité").contains(entete)
                ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT));
        }
        for (LigneResponse ligne : avenant.lignes()) {
            Phrase designation = new Phrase(ligne.designation(), NORMAL);
            if (ligne.description() != null) {
                designation.add(Chunk.NEWLINE);
                designation.add(new Chunk(ligne.description(), DISCRET));
            }
            PdfPCell celluleDesignation = new PdfPCell(designation);
            celluleDesignation.setBorderColor(Color.LIGHT_GRAY);
            celluleDesignation.setPadding(4);
            table.addCell(celluleDesignation);
            table.addCell(cellule(ligne.pieceId() == null ? "Général"
                : document.pieces().getOrDefault(ligne.pieceId(), ""), DISCRET, Element.ALIGN_LEFT));
            table.addCell(cellule(document.lots().getOrDefault(ligne.lotId(), ""), DISCRET, Element.ALIGN_LEFT));
            table.addCell(cellule(nombre(ligne.quantite()), NORMAL, Element.ALIGN_RIGHT));
            table.addCell(cellule(ligne.unite(), NORMAL, Element.ALIGN_LEFT));
            table.addCell(cellule(euros(ligne.prixUnitaireHt()), NORMAL, Element.ALIGN_RIGHT));
            table.addCell(cellule(nombre(ligne.tauxTva()) + " %", NORMAL, Element.ALIGN_RIGHT));
            table.addCell(cellule(euros(ligne.montantHt()), NORMAL, Element.ALIGN_RIGHT));
        }
        pdf.add(table);

        Totaux totaux = avenant.totaux();
        PdfPTable tableTotaux = new PdfPTable(2);
        tableTotaux.setWidthPercentage(45);
        tableTotaux.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tableTotaux.setSpacingBefore(10);
        total(tableTotaux, "Total HT de l'avenant", totaux.totalHt(), NORMAL);
        for (Totaux.Tva tva : totaux.tva()) {
            total(tableTotaux, "TVA " + nombre(tva.taux()) + " %", tva.montant(), NORMAL);
        }
        total(tableTotaux, "Total TTC de l'avenant", totaux.totalTtc(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
        pdf.add(tableTotaux);

        Paragraph decision;
        if (avenant.statut() == StatutAvenant.VALIDE || avenant.statut() == StatutAvenant.REFUSE) {
            decision = new Paragraph((avenant.statut() == StatutAvenant.VALIDE ? "VALIDÉ" : "REFUSÉ")
                + " le " + date(avenant.decideAt()) + " par " + avenant.decidePar()
                + (avenant.decisionCommentaire() != null ? " — « " + avenant.decisionCommentaire() + " »" : ""), GRAS);
        } else {
            decision = new Paragraph("Date et signature du client, précédées de la mention « Bon pour accord » :", NORMAL);
        }
        decision.setSpacingBefore(24);
        pdf.add(decision);
        pdf.close();
        return out.toByteArray();
    }

    private static String date(Instant instant) {
        return instant == null ? "" : instant.atZone(ZoneId.of("Europe/Paris")).format(DATE);
    }

    private static void total(PdfPTable table, String libelle, BigDecimal montant, Font font) {
        PdfPCell gauche = cellule(libelle, font, Element.ALIGN_RIGHT);
        PdfPCell droite = cellule(euros(montant), font, Element.ALIGN_RIGHT);
        gauche.setBorder(PdfPCell.NO_BORDER);
        droite.setBorder(PdfPCell.NO_BORDER);
        table.addCell(gauche);
        table.addCell(droite);
    }

    private static PdfPCell cellule(String texte, Font font, int alignement) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(alignement);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(4);
        return cell;
    }

    private static String euros(BigDecimal montant) {
        return com.faro.devis.DevisPdfService.euros(montant);
    }

    private static String nombre(BigDecimal valeur) {
        return com.faro.devis.DevisPdfService.nombre(valeur);
    }
}
