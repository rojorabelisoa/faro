package com.faro.achat;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
import org.springframework.transaction.annotation.Transactional;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.avenant.StatutAvenant;
import com.faro.devis.DevisPdfService;
import com.faro.devis.LigneDevis;
import com.faro.devis.LigneDevisRepository;
import com.faro.lot.Lot;
import com.faro.lot.LotService;
import com.faro.organisation.Organisation;

/**
 * Bon de commande d'un lot pour un fournisseur : les ouvrages retenus (devis + avenants validés) et leurs quantités,
 * sans les prix de vente. À compléter par le conducteur de travaux (références, prix d'achat).
 */
@Service
@Transactional(readOnly = true)
public class BonCommandeService {

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font DISCRET = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

    private final AffaireService affaireService;
    private final LotService lotService;
    private final LigneDevisRepository lignes;

    public BonCommandeService(AffaireService affaireService, LotService lotService, LigneDevisRepository lignes) {
        this.affaireService = affaireService;
        this.lotService = lotService;
        this.lignes = lignes;
    }

    public byte[] pdf(Organisation organisation, UUID affaireId, UUID lotId, String fournisseur) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        Lot lot = lotService.get(organisation, lotId);
        List<LigneDevis> aCommander = Stream.concat(
                lignes.findByAffaireIdAndAvenantIsNullOrderByOrdreAsc(affaire.getId()).stream()
                    .filter(LigneDevis::isIncluse),
                lignes.findByAffaireIdAndAvenantIsNotNull(affaire.getId()).stream()
                    .filter(l -> l.getAvenant().getStatut() == StatutAvenant.VALIDE))
            .filter(l -> l.getLot().getId().equals(lot.getId()))
            .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(organisation.getNom(), GRAS));
        Paragraph titre = new Paragraph("Bon de commande — " + lot.getCode() + " · " + lot.getNom(), TITRE);
        titre.setSpacingBefore(8);
        document.add(titre);
        if (fournisseur != null && !fournisseur.isBlank()) {
            document.add(new Paragraph("Fournisseur : " + fournisseur.trim(), NORMAL));
        }
        document.add(new Paragraph("Chantier : " + affaire.getNom()
            + (affaire.getAdresse() != null ? " — livraison : " + affaire.getAdresse() : ""), NORMAL));
        document.add(new Paragraph("Édité le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), DISCRET));

        PdfPTable table = new PdfPTable(new float[] {50, 20, 12, 18});
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);
        table.setHeaderRows(1);
        for (String entete : List.of("Désignation", "Pièce", "Quantité", "Référence / prix")) {
            table.addCell(cellule(entete, GRAS, Element.ALIGN_LEFT));
        }
        for (LigneDevis ligne : aCommander) {
            String designation = ligne.getDesignation() + (ligne.getDescription() != null ? "\n" + ligne.getDescription() : "");
            table.addCell(cellule(designation, NORMAL, Element.ALIGN_LEFT));
            table.addCell(cellule(ligne.getPiece() != null ? ligne.getPiece().getNom() : "Général", DISCRET, Element.ALIGN_LEFT));
            table.addCell(cellule(DevisPdfService.nombre(ligne.getQuantite()) + " " + ligne.getUnite(), NORMAL,
                Element.ALIGN_RIGHT));
            table.addCell(cellule("", NORMAL, Element.ALIGN_LEFT));
        }
        if (aCommander.isEmpty()) {
            PdfPCell vide = cellule("Aucun ouvrage retenu pour ce lot.", DISCRET, Element.ALIGN_LEFT);
            vide.setColspan(4);
            table.addCell(vide);
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private static PdfPCell cellule(String texte, Font font, int alignement) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(alignement);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(5);
        return cell;
    }
}
