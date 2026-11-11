package com.faro.visite;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
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
import com.faro.affaire.Piece;
import com.faro.affaire.PieceService;
import com.faro.organisation.Organisation;

/** Compte-rendu de visite en PDF : observations et photos, pièce par pièce. */
@Service
@Transactional(readOnly = true)
public class VisitePdfService {

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(31, 95, 168));
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font DISCRET = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AffaireService affaireService;
    private final PieceService pieceService;
    private final ObservationRepository observations;
    private final PhotoRepository photos;

    public VisitePdfService(AffaireService affaireService, PieceService pieceService,
            ObservationRepository observations, PhotoRepository photos) {
        this.affaireService = affaireService;
        this.pieceService = pieceService;
        this.observations = observations;
        this.photos = photos;
    }

    public byte[] pdf(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        List<Piece> pieces = pieceService.list(organisation, affaireId);
        List<Observation> notes = observations.findByAffaireIdOrderByCreatedAtAsc(affaire.getId());
        List<Photo> images = photos.findByAffaireIdOrderByCreatedAtAsc(affaire.getId());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(organisation.getNom(), GRAS));
        Paragraph titre = new Paragraph("Compte-rendu de visite — " + affaire.getNom(), TITRE);
        titre.setSpacingBefore(8);
        document.add(titre);
        List<String> infos = new ArrayList<>();
        for (String info : new String[] {affaire.getClientNom(), affaire.getAdresse()}) {
            if (info != null && !info.isBlank()) {
                infos.add(info);
            }
        }
        document.add(new Paragraph(String.join(" · ", infos), NORMAL));
        document.add(new Paragraph("Édité le " + LocalDate.now().format(DATE), DISCRET));
        if (affaire.getNotes() != null && !affaire.getNotes().isBlank()) {
            Paragraph notesAffaire = new Paragraph(affaire.getNotes(), NORMAL);
            notesAffaire.setSpacingBefore(8);
            document.add(notesAffaire);
        }

        List<Piece> sections = new ArrayList<>();
        sections.add(null);
        sections.addAll(pieces);
        for (Piece piece : sections) {
            UUID pieceId = piece != null ? piece.getId() : null;
            List<Observation> notesPiece = notes.stream().filter(o -> Objects.equals(id(o.getPiece()), pieceId)).toList();
            List<Photo> imagesPiece = images.stream().filter(p -> Objects.equals(id(p.getPiece()), pieceId)).toList();
            if (notesPiece.isEmpty() && imagesPiece.isEmpty() && (piece == null || piece.getSurfaceM2() == null)) {
                continue;
            }
            Paragraph titreSection = new Paragraph(piece != null ? piece.getNom() : "Généralités", SECTION);
            titreSection.setSpacingBefore(14);
            titreSection.setSpacingAfter(4);
            document.add(titreSection);
            if (piece != null && piece.getSurfaceM2() != null) {
                document.add(new Paragraph(metres(piece), DISCRET));
            }
            for (Observation note : notesPiece) {
                Paragraph paragraphe = new Paragraph("• " + note.getTexte(), NORMAL);
                paragraphe.setIndentationLeft(8);
                paragraphe.setSpacingAfter(3);
                document.add(paragraphe);
            }
            if (!imagesPiece.isEmpty()) {
                document.add(grillePhotos(imagesPiece));
            }
        }
        document.close();
        return out.toByteArray();
    }

    private static UUID id(Piece piece) {
        return piece != null ? piece.getId() : null;
    }

    private static String metres(Piece piece) {
        StringBuilder texte = new StringBuilder("Sol : " + piece.getSurfaceM2().stripTrailingZeros().toPlainString() + " m²");
        if (piece.getPerimetreMl() != null) {
            texte.append(" · périmètre : ").append(piece.getPerimetreMl().stripTrailingZeros().toPlainString()).append(" ml");
        }
        if (piece.getHauteurM() != null) {
            texte.append(" · hauteur : ").append(piece.getHauteurM().stripTrailingZeros().toPlainString()).append(" m");
        }
        if (piece.getSurfaceMursM2() != null) {
            texte.append(" · murs : ").append(piece.getSurfaceMursM2().stripTrailingZeros().toPlainString()).append(" m²");
        }
        return texte.toString().replace('.', ',');
    }

    /** Deux photos par ligne, avec leur légende. */
    private static PdfPTable grillePhotos(List<Photo> images) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        for (Photo photo : images) {
            PdfPCell cellule;
            try {
                Image image = Image.getInstance(photo.getDonnees());
                image.scaleToFit(250, 190);
                cellule = new PdfPCell();
                cellule.addElement(image);
            } catch (Exception e) {
                // Format non lisible par le moteur PDF (ex. WebP) : on garde la légende.
                cellule = new PdfPCell(new Phrase("[photo non affichable]", DISCRET));
            }
            if (photo.getLegende() != null) {
                cellule.addElement(new Paragraph(photo.getLegende(), DISCRET));
            }
            cellule.setBorder(PdfPCell.NO_BORDER);
            cellule.setPadding(4);
            cellule.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellule);
        }
        if (images.size() % 2 == 1) {
            PdfPCell vide = new PdfPCell();
            vide.setBorder(PdfPCell.NO_BORDER);
            table.addCell(vide);
        }
        return table;
    }
}
