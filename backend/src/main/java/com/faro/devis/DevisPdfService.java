package com.faro.devis;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

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
import org.springframework.transaction.annotation.Transactional;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.devis.DevisResponse.LigneResponse;
import com.faro.organisation.Organisation;

/** Devis au format PDF, présenté par pièce (pour le client) ou par lot (pour le chantier). */
@Service
@Transactional(readOnly = true)
public class DevisPdfService {

    public enum Vue { PIECE, LOT }

    private static final Color FOND_GROUPE = new Color(231, 239, 249);
    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font DISCRET = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final float[] LARGEURS = {38, 18, 7, 7, 11, 7, 12};

    private final DevisService devisService;
    private final AffaireService affaireService;

    public DevisPdfService(DevisService devisService, AffaireService affaireService) {
        this.devisService = devisService;
        this.affaireService = affaireService;
    }

    public byte[] pdf(Organisation organisation, UUID affaireId, Vue vue) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        DevisResponse devis = devisService.devis(organisation, affaireId);
        return rendre(new EnTete(organisation.getNom(), affaire.getNom(), affaire.getClientNom(), affaire.getAdresse(),
            "Établi le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), devis, vue);
    }

    /** Informations de l'en-tête ; mention : date ou numéro de version. */
    public record EnTete(String entreprise, String affaire, String client, String adresse, String mention) {
    }

    public byte[] rendre(EnTete enTete, DevisResponse devis, Vue vue) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph(enTete.entreprise(), GRAS));
        Paragraph titre = new Paragraph("Devis — " + enTete.affaire(), TITRE);
        titre.setSpacingBefore(8);
        document.add(titre);
        document.add(new Paragraph(String.join(" · ", nonVides(enTete.client(), enTete.adresse())), NORMAL));
        document.add(new Paragraph(enTete.mention()
            + (vue == Vue.PIECE ? " — présentation par pièce" : " — présentation par lot"), DISCRET));

        PdfPTable table = new PdfPTable(LARGEURS.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(14);
        table.setWidths(LARGEURS);
        table.setHeaderRows(1);
        List<String> entetes = List.of("Désignation", vue == Vue.PIECE ? "Lot" : "Pièce", "Qté", "Unité", "PU HT", "TVA",
            "Total HT");
        List<String> aGauche = List.of("Désignation", "Lot", "Pièce", "Unité");
        for (String entete : entetes) {
            table.addCell(cellule(entete, GRAS, aGauche.contains(entete) ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT, null));
        }

        for (Groupe groupe : groupes(devis, vue)) {
            PdfPCell titreGroupe = cellule(groupe.titre(), GRAS, Element.ALIGN_LEFT, FOND_GROUPE);
            titreGroupe.setColspan(6);
            table.addCell(titreGroupe);
            table.addCell(cellule(euros(groupe.totalHt()), GRAS, Element.ALIGN_RIGHT, FOND_GROUPE));
            for (LigneResponse ligne : groupe.lignes()) {
                Phrase designation = new Phrase(ligne.designation(), NORMAL);
                if (ligne.description() != null && !ligne.description().isBlank()) {
                    designation.add(Chunk.NEWLINE);
                    designation.add(new Chunk(ligne.description(), DISCRET));
                }
                PdfPCell celluleDesignation = new PdfPCell(designation);
                celluleDesignation.setBorderColor(Color.LIGHT_GRAY);
                celluleDesignation.setPadding(4);
                table.addCell(celluleDesignation);
                table.addCell(cellule(vue == Vue.PIECE ? nomLot(devis, ligne.lotId()) : nomPiece(devis, ligne.pieceId()),
                    DISCRET, Element.ALIGN_LEFT, null));
                table.addCell(cellule(nombre(ligne.quantite()), NORMAL, Element.ALIGN_RIGHT, null));
                table.addCell(cellule(ligne.unite(), NORMAL, Element.ALIGN_LEFT, null));
                table.addCell(cellule(euros(ligne.prixUnitaireHt()), NORMAL, Element.ALIGN_RIGHT, null));
                table.addCell(cellule(nombre(ligne.tauxTva()) + " %", NORMAL, Element.ALIGN_RIGHT, null));
                table.addCell(cellule(euros(ligne.montantHt()), NORMAL, Element.ALIGN_RIGHT, null));
            }
        }
        document.add(table);

        PdfPTable totaux = new PdfPTable(2);
        totaux.setWidthPercentage(45);
        totaux.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totaux.setSpacingBefore(10);
        ligneTotal(totaux, "Total HT", devis.totalHt(), NORMAL);
        for (DevisResponse.TotalTva tva : devis.tva()) {
            ligneTotal(totaux, "TVA " + nombre(tva.taux()) + " % sur " + euros(tva.baseHt()), tva.montant(), NORMAL);
        }
        ligneTotal(totaux, "Total TTC", devis.totalTtc(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
        document.add(totaux);

        Paragraph accord = new Paragraph("Date et signature du client, précédées de la mention « Bon pour accord » :",
            NORMAL);
        accord.setSpacingBefore(30);
        document.add(accord);

        document.close();
        return out.toByteArray();
    }

    record Groupe(String titre, BigDecimal totalHt, List<LigneResponse> lignes) {
    }

    /** Mêmes regroupements que l'écran : les lignes gardent l'ordre choisi par l'utilisateur. */
    static List<Groupe> groupes(DevisResponse devis, Vue vue) {
        List<Groupe> groupes = new ArrayList<>();
        if (vue == Vue.PIECE) {
            for (DevisResponse.TotalPiece piece : devis.parPiece()) {
                List<LigneResponse> lignes = devis.lignes().stream().filter(LigneResponse::incluse)
                    .filter(l -> Objects.equals(l.pieceId(), piece.pieceId())).toList();
                if (!lignes.isEmpty()) {
                    groupes.add(new Groupe(piece.nom(), piece.totalHt(), lignes));
                }
            }
        } else {
            for (DevisResponse.TotalLot lot : devis.parLot()) {
                groupes.add(new Groupe(lot.code() + " · " + lot.nom(), lot.totalHt(),
                    devis.lignes().stream().filter(LigneResponse::incluse)
                        .filter(l -> l.lotId().equals(lot.lotId())).toList()));
            }
        }
        return groupes;
    }

    private static String nomLot(DevisResponse devis, UUID lotId) {
        return devis.parLot().stream().filter(l -> l.lotId().equals(lotId)).findFirst()
            .map(l -> l.code() + " · " + l.nom()).orElse("");
    }

    private static String nomPiece(DevisResponse devis, UUID pieceId) {
        return devis.parPiece().stream().filter(p -> Objects.equals(p.pieceId(), pieceId)).findFirst()
            .map(DevisResponse.TotalPiece::nom).orElse("");
    }

    private static void ligneTotal(PdfPTable table, String libelle, BigDecimal montant, Font font) {
        PdfPCell gauche = cellule(libelle, font, Element.ALIGN_RIGHT, null);
        PdfPCell droite = cellule(euros(montant), font, Element.ALIGN_RIGHT, null);
        gauche.setBorder(PdfPCell.NO_BORDER);
        droite.setBorder(PdfPCell.NO_BORDER);
        table.addCell(gauche);
        table.addCell(droite);
    }

    private static PdfPCell cellule(String texte, Font font, int alignement, Color fond) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(alignement);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(4);
        if (fond != null) {
            cell.setBackgroundColor(fond);
        }
        return cell;
    }

    private static List<String> nonVides(String... valeurs) {
        return java.util.Arrays.stream(valeurs).filter(v -> v != null && !v.isBlank()).toList();
    }

    /** Les polices PDF standard n'ont pas l'espace fine insécable utilisée par Java en français. */
    private static String sansEspacesFines(String texte) {
        return texte.replace(' ', ' ').replace(' ', ' ');
    }

    static String euros(BigDecimal montant) {
        return sansEspacesFines(NumberFormat.getCurrencyInstance(Locale.FRANCE).format(montant));
    }

    static String nombre(BigDecimal valeur) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMaximumFractionDigits(3);
        return sansEspacesFines(format.format(valeur));
    }
}
