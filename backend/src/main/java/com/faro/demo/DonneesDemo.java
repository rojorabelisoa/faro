package com.faro.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireRequest;
import com.faro.affaire.AffaireService;
import com.faro.affaire.NiveauTravaux;
import com.faro.affaire.Piece;
import com.faro.affaire.PieceRequest;
import com.faro.affaire.PieceService;
import com.faro.affaire.StatutAffaire;
import com.faro.affaire.TypeBien;
import com.faro.affaire.TypePiece;
import com.faro.avenant.Avenant;
import com.faro.avenant.AvenantRequest;
import com.faro.avenant.AvenantService;
import com.faro.avenant.DecisionRequest;
import com.faro.avenant.StatutAvenant;
import com.faro.bibliotheque.Ouvrage;
import com.faro.bibliotheque.OuvrageRequest;
import com.faro.bibliotheque.OuvrageService;
import com.faro.client.EspaceClientService;
import com.faro.devis.DevisService;
import com.faro.devis.Gamme;
import com.faro.devis.LigneDepuisOuvrageRequest;
import com.faro.devis.LigneDevisRequest;
import com.faro.investisseur.BilanDtos.BilanRequest;
import com.faro.investisseur.BilanService;
import com.faro.lot.Lot;
import com.faro.lot.LotService;
import com.faro.marge.DepenseRequest;
import com.faro.marge.MargeService;
import com.faro.marge.NatureDepense;
import com.faro.organisation.Organisation;
import com.faro.organisation.OrganisationService;
import com.faro.organisation.RoleEntreprise;
import com.faro.user.AppUser;
import com.faro.user.AppUserRepository;
import com.faro.version.VersionService;
import com.faro.visite.ObservationRequest;
import com.faro.visite.VisiteService;

/**
 * Données de démonstration (profil Docker « complet », propriété faro.demo.donnees=true) : une entreprise de
 * rénovation lyonnaise, sa bibliothèque de prix et 7 affaires à tous les stades, rattachées aux comptes
 * Keycloak de démo (identifiants fixes du realm). Chargées une seule fois, jamais si la base contient déjà des affaires.
 */
@Component
@ConditionalOnProperty(name = "faro.demo.donnees", havingValue = "true")
public class DonneesDemo implements ApplicationRunner {

    static final String SUBJECT_DEMO = "00000000-0000-4000-8000-000000000001";
    static final String SUBJECT_ADMIN = "00000000-0000-4000-8000-000000000002";
    private static final Logger LOG = LoggerFactory.getLogger(DonneesDemo.class);
    private static final String AUTEUR = "Demo Faro";

    private final JdbcTemplate jdbc;
    private final OrganisationService organisations;
    private final AppUserRepository users;
    private final LotService lotService;
    private final OuvrageService ouvrageService;
    private final AffaireService affaireService;
    private final PieceService pieceService;
    private final DevisService devisService;
    private final VisiteService visiteService;
    private final VersionService versionService;
    private final AvenantService avenantService;
    private final MargeService margeService;
    private final EspaceClientService espaceClientService;
    private final BilanService bilanService;

    private Organisation organisation;
    private final Map<String, Lot> lots = new HashMap<>();
    private final Map<String, Ouvrage> ouvrages = new HashMap<>();

    public DonneesDemo(JdbcTemplate jdbc, OrganisationService organisations, AppUserRepository users, LotService lotService,
            OuvrageService ouvrageService, AffaireService affaireService, PieceService pieceService,
            DevisService devisService, VisiteService visiteService, VersionService versionService,
            AvenantService avenantService, MargeService margeService, EspaceClientService espaceClientService,
            BilanService bilanService) {
        this.jdbc = jdbc;
        this.organisations = organisations;
        this.users = users;
        this.lotService = lotService;
        this.ouvrageService = ouvrageService;
        this.affaireService = affaireService;
        this.pieceService = pieceService;
        this.devisService = devisService;
        this.visiteService = visiteService;
        this.versionService = versionService;
        this.avenantService = avenantService;
        this.margeService = margeService;
        this.espaceClientService = espaceClientService;
        this.bilanService = bilanService;
    }

    @Override
    public void run(ApplicationArguments args) {
        charger();
    }

    /** @return true si les données ont été chargées, false si la base n'était pas vide. */
    public boolean charger() {
        Integer affaires = jdbc.queryForObject("SELECT count(*) FROM affaire", Integer.class);
        if (affaires != null && affaires > 0 || users.findBySubject(SUBJECT_DEMO).isPresent()) {
            LOG.info("Données de démonstration non chargées : la base contient déjà des données.");
            return false;
        }
        organisation = organisations.create("Rénov Lyon (démo)");
        utilisateur(SUBJECT_DEMO, "Demo Faro", "demo@faro.local", RoleEntreprise.GERANT);
        utilisateur(SUBJECT_ADMIN, "Admin Faro", "admin@faro.local", RoleEntreprise.CHARGE_ETUDES);
        lotService.list(organisation).forEach(l -> lots.put(l.getCode(), l));
        bibliotheque();
        renovationT3();
        maisonEcully();
        studioCroixRousse();
        appartementBrotteaux();
        loftConfluence();
        extensionCaluire();
        t2Guillotiere();
        LOG.info("Données de démonstration chargées : 7 affaires pour « Rénov Lyon (démo) ».");
        return true;
    }

    private void utilisateur(String subject, String nom, String email, RoleEntreprise role) {
        AppUser user = new AppUser(subject);
        user.setDisplayName(nom);
        user.setEmail(email);
        user.setOrganisation(organisation);
        user.setRoleEntreprise(role);
        users.save(user);
    }

    // --- Bibliothèque de prix -------------------------------------------------------------------

    private void bibliotheque() {
        Object[][] lignes = {
            {"protection", "01", "Protection des sols et des parties communes", "forfait", "380", "10", "190"},
            {"depose-cuisine", "02", "Dépose de cuisine existante", "forfait", "450", "10", "260"},
            {"demolition", "02", "Démolition de cloison non porteuse", "m²", "38", "10", "22"},
            {"depose-carrelage", "02", "Dépose de carrelage et faïence", "m²", "28", "10", "16"},
            {"cloison", "04", "Cloison plaque de plâtre BA13 sur ossature", "m²", "52", "10", "31"},
            {"doublage", "04", "Doublage isolant PSE + BA13", "m²", "58", "5.5", "34"},
            {"porte", "05", "Porte intérieure postformée", "U", "420", "10", "240"},
            {"wc", "07", "Fourniture et pose WC suspendu", "U", "690", "10", "410"},
            {"vasque", "07", "Meuble vasque 80 cm avec mitigeur", "U", "850", "10", "520"},
            {"douche", "07", "Receveur extra-plat et paroi de douche", "U", "1350", "10", "820"},
            {"alim-cuisine", "07", "Alimentation et évacuation cuisine", "forfait", "520", "10", "290"},
            {"prise", "08", "Prise de courant 16A", "U", "85", "10", "42"},
            {"lumiere", "08", "Point lumineux plafond", "U", "110", "10", "55"},
            {"tableau", "08", "Tableau électrique complet aux normes", "U", "1850", "10", "1100"},
            {"vmc", "09", "VMC hygroréglable B", "U", "980", "5.5", "590"},
            {"carrelage", "10", "Carrelage sol grès cérame 60x60", "m²", "72.40", "10", "44"},
            {"faience", "10", "Faïence murale 30x60", "m²", "65", "10", "39"},
            {"parquet", "11", "Parquet chêne contrecollé, pose flottante", "m²", "78", "10", "47"},
            {"peinture", "12", "Peinture murs et plafonds, 2 couches", "m²", "24", "10", "13"},
            {"pose-cuisine", "13", "Pose de cuisine équipée (hors fourniture)", "forfait", "1600", "10", "950"},
            {"gravats", "14", "Évacuation des gravats en décharge", "forfait", "480", "10", "290"},
        };
        for (Object[] l : lignes) {
            ouvrages.put((String) l[0], ouvrageService.create(organisation, new OuvrageRequest(lots.get((String) l[1]).getId(),
                (String) l[2], null, (String) l[3], dec((String) l[4]), dec((String) l[5]), dec((String) l[6]))));
        }
    }

    // --- Affaires -------------------------------------------------------------------------------

    private void renovationT3() {
        Affaire a = affaire("Rénovation T3 – rue Victor Hugo", "M. et Mme Martin", "martin@example.fr", "06 12 34 56 78",
            "12 rue Victor Hugo, 69002 Lyon", TypeBien.APPARTEMENT, "Immeuble 1900, 3e étage avec ascenseur. Code 4512B.");
        Piece cuisine = piece(a, "Cuisine", "11", "13.6", "2.7", TypePiece.CUISINE, NiveauTravaux.RENOVATION);
        Piece sdb = piece(a, "Salle de bains", "6", "10", "2.5", TypePiece.SALLE_DE_BAINS, NiveauTravaux.RENOVATION_LOURDE);
        Piece sejour = piece(a, "Séjour", "24", "20", "2.7", TypePiece.SEJOUR, NiveauTravaux.RAFRAICHISSEMENT);
        Piece chambre = piece(a, "Chambre 1", "12", "14", "2.7", TypePiece.CHAMBRE, NiveauTravaux.RAFRAICHISSEMENT);
        note(a, null, "Accès par la cour, stationnement difficile : prévoir une autorisation de voirie pour la benne.");
        note(a, cuisine, "Cuisine fermée à ouvrir sur le séjour : cloison non porteuse (vérifiée avec le syndic).");
        note(a, sdb, "Baignoire à remplacer par une douche à l'italienne, évacuation à reprendre.");
        ligne(a, null, "protection", "1");
        ligne(a, cuisine, "depose-cuisine", "1");
        ligne(a, cuisine, "demolition", "7.5");
        ligne(a, cuisine, "alim-cuisine", "1");
        ligne(a, cuisine, "faience", "4.5");
        ligne(a, cuisine, "pose-cuisine", "1");
        ligne(a, sdb, "depose-carrelage", "22");
        ligne(a, sdb, "douche", "1");
        ligne(a, sdb, "wc", "1");
        ligne(a, sdb, "vasque", "1");
        ligne(a, sdb, "carrelage", "6");
        ligne(a, sdb, "faience", "16");
        ligne(a, sdb, "vmc", "1");
        ligne(a, sejour, "peinture", "78");
        ligne(a, chambre, "peinture", "50");
        ligne(a, chambre, "parquet", "12");
        ligne(a, null, "gravats", "1");
        versionService.figer(organisation, a.getId(), "Devis envoyé au client", AUTEUR);
        versionService.passerEnChantier(organisation, a.getId(), null, AUTEUR);
        Avenant prises = avenant(a, "Prises supplémentaires séjour", "Demande de Mme Martin lors de la visite de chantier",
            new String[] {"prise", "4"});
        envoyerEtDecider(a, prises, StatutAvenant.VALIDE, "Mme Martin");
        Avenant porte = avenant(a, "Remplacement de la porte palière", "Porte existante voilée, découverte en cours de chantier",
            new String[] {"porte", "1"});
        avenantService.envoyer(organisation, a.getId(), porte.getId());
        depense(a, "14", NatureDepense.LOCATION, "Benne 8 m³ (réservation)", "Benne Express", "360", 0);
        espaceClientService.creerLien(organisation, a.getId());
        // Signé aujourd'hui : le tableau de bord de démo montre une signature du mois.
        dater(a, 45, 0, 0);
    }

    private void maisonEcully() {
        Affaire a = affaire("Maison des Pins – rénovation intérieure", "Famille Bernard", "bernard@example.fr", "06 98 76 54 32",
            "8 chemin des Pins, 69130 Écully", TypeBien.MAISON, "Maison années 70, isolation des murs par l'intérieur.");
        Piece sejour = piece(a, "Séjour", "38", "26", "2.5", TypePiece.SEJOUR, NiveauTravaux.RENOVATION);
        Piece ch1 = piece(a, "Chambre 1", "13", "14.6", "2.5", TypePiece.CHAMBRE, NiveauTravaux.RENOVATION);
        Piece ch2 = piece(a, "Chambre 2", "11", "13.4", "2.5", TypePiece.CHAMBRE, NiveauTravaux.RENOVATION);
        Piece entree = piece(a, "Entrée et couloir", "9", "16", "2.5", TypePiece.ENTREE_DEGAGEMENT, NiveauTravaux.RENOVATION);
        ligne(a, null, "protection", "1");
        ligne(a, sejour, "doublage", "52");
        ligne(a, ch1, "doublage", "24");
        ligne(a, ch2, "doublage", "21");
        ligne(a, null, "tableau", "1");
        ligne(a, sejour, "prise", "8");
        ligne(a, sejour, "lumiere", "3");
        ligne(a, ch1, "prise", "4");
        ligne(a, ch2, "prise", "4");
        ligne(a, sejour, "parquet", "38");
        ligne(a, ch1, "parquet", "13");
        ligne(a, ch2, "parquet", "11");
        ligne(a, entree, "carrelage", "9");
        ligne(a, sejour, "peinture", "100");
        ligne(a, ch1, "peinture", "50");
        ligne(a, ch2, "peinture", "45");
        ligne(a, entree, "porte", "4");
        ligne(a, null, "gravats", "1");
        versionService.passerEnChantier(organisation, a.getId(), null, AUTEUR);
        Avenant vmc = avenant(a, "VMC hygroréglable", "Ventilation insuffisante après isolation", new String[] {"vmc", "1"});
        envoyerEtDecider(a, vmc, StatutAvenant.VALIDE, "M. Bernard");
        depense(a, "04", NatureDepense.SOUS_TRAITANCE, "Plaquiste – doublages", "Isol'Rhône", "4200", 35);
        depense(a, "08", NatureDepense.SOUS_TRAITANCE, "Électricien – tableau et prises", "Élec Ouest Lyonnais", "2650", 28);
        depense(a, "11", NatureDepense.MATERIAUX, "Parquet chêne 62 m²", "Lapeyre Écully", "2950", 20);
        depense(a, "12", NatureDepense.MAIN_OEUVRE, "Peinture (5 jours)", null, "1650", 10);
        dater(a, 80, 40, 22);
    }

    private void studioCroixRousse() {
        Affaire a = affaire("Studio Croix-Rousse – achat-revente", "Mme Leroy (marchand de biens)", "leroy@example.fr", null,
            "41 rue d'Austerlitz, 69004 Lyon", TypeBien.APPARTEMENT, "Opération de marchand de biens : revente prévue à 6 mois.");
        Piece piece = piece(a, "Pièce principale", "22", "19", "2.8", TypePiece.SEJOUR, NiveauTravaux.RENOVATION);
        Piece sdb = piece(a, "Salle d'eau", "3.5", "7.6", "2.5", TypePiece.SALLE_DE_BAINS, NiveauTravaux.RENOVATION_LOURDE);
        Piece coin = piece(a, "Coin cuisine", "5", "9", "2.8", TypePiece.CUISINE, NiveauTravaux.RENOVATION);
        ligne(a, null, "protection", "1");
        ligne(a, piece, "parquet", "22");
        ligne(a, piece, "peinture", "75");
        ligne(a, piece, "prise", "5");
        ligne(a, sdb, "douche", "1");
        ligne(a, sdb, "wc", "1");
        ligne(a, sdb, "vasque", "1");
        ligne(a, sdb, "faience", "12");
        ligne(a, coin, "alim-cuisine", "1");
        ligne(a, coin, "pose-cuisine", "1");
        ligne(a, null, "gravats", "1");
        versionService.passerEnChantier(organisation, a.getId(), null, AUTEUR);
        depense(a, "07", NatureDepense.SOUS_TRAITANCE, "Plombier – salle d'eau", "Plomberie Rhône", "2100", 120);
        depense(a, "11", NatureDepense.MATERIAUX, "Parquet et plinthes", "Point P Lyon 4", "1050", 115);
        depense(a, "12", NatureDepense.MAIN_OEUVRE, "Peinture et finitions", null, "1100", 100);
        depense(a, "13", NatureDepense.SOUS_TRAITANCE, "Pose kitchenette", "Cuisines du Rhône", "1150", 100);
        bilanService.enregistrer(organisation, a.getId(),
            new BilanRequest(dec("118000"), dec("9400"), dec("2500"), dec("168000"), 6));
        affaireService.update(organisation, a.getId(), requete(a, StatutAffaire.TERMINEE));
        dater(a, 160, 130, null);
    }

    private void appartementBrotteaux() {
        Affaire a = affaire("Appartement Brotteaux – cuisine et salle de bains", "M. Petit", "petit@example.fr",
            "07 11 22 33 44", "5 boulevard des Brotteaux, 69006 Lyon", TypeBien.APPARTEMENT,
            "Le client hésite sur la finition de la salle de bains : trois gammes proposées.");
        Piece cuisine = piece(a, "Cuisine", "9", "12", "2.6", TypePiece.CUISINE, NiveauTravaux.RENOVATION);
        Piece sdb = piece(a, "Salle de bains", "5.5", "9.6", "2.5", TypePiece.SALLE_DE_BAINS, NiveauTravaux.RENOVATION);
        ligne(a, null, "protection", "1");
        ligne(a, cuisine, "depose-cuisine", "1");
        ligne(a, cuisine, "alim-cuisine", "1");
        ligne(a, cuisine, "pose-cuisine", "1");
        ligne(a, cuisine, "prise", "6");
        ligne(a, sdb, "depose-carrelage", "20");
        ligne(a, sdb, "wc", "1");
        ligne(a, sdb, "vasque", "1");
        gamme(a, sdb, "Faïence 20x20 blanche", "45", "26", Gamme.ECO);
        gamme(a, sdb, "Faïence 30x60 rectifiée", "65", "39", Gamme.STANDARD);
        gamme(a, sdb, "Zellige artisanal fait main", "140", "88", Gamme.PREMIUM);
        ligne(a, null, "gravats", "1");
        versionService.figer(organisation, a.getId(), "Devis envoyé au client", AUTEUR);
        affaireService.update(organisation, a.getId(), requete(a, StatutAffaire.DOSSIER_TRAVAUX));
        espaceClientService.creerLien(organisation, a.getId());
        dater(a, 40, null, null);
    }

    private void loftConfluence() {
        Affaire a = affaire("Loft Confluence", "Mme Garnier", "garnier@example.fr", null,
            "18 quai Rambaud, 69002 Lyon", TypeBien.APPARTEMENT, "Plateau brut de 85 m² à aménager.");
        piece(a, "Séjour – cuisine ouverte", "45", null, "3", TypePiece.SEJOUR, NiveauTravaux.RENOVATION_LOURDE);
        piece(a, "Chambre parentale", "16", null, "3", TypePiece.CHAMBRE, NiveauTravaux.RENOVATION_LOURDE);
        piece(a, "Salle de bains", "8", null, "3", TypePiece.SALLE_DE_BAINS, NiveauTravaux.RENOVATION_LOURDE);
        piece(a, "WC", "2", null, "3", TypePiece.WC, NiveauTravaux.RENOVATION_LOURDE);
        note(a, null, "Plateau brut : tout est à créer (cloisons, réseaux, sols). Estimation rapide envoyée au client.");
        affaireService.update(organisation, a.getId(), requete(a, StatutAffaire.ESTIMATION));
        // Sans nouvelle depuis 20 jours : apparaît « à relancer » sur le tableau de bord.
        jdbc.update("UPDATE affaire SET created_at = now() - interval '30 days', updated_at = now() - interval '20 days' WHERE id = ?",
            a.getId());
    }

    private void extensionCaluire() {
        Affaire a = affaire("Maison Caluire – extension", "M. et Mme Rossi", "rossi@example.fr", "06 55 44 33 22",
            "27 montée des Forts, 69300 Caluire-et-Cuire", TypeBien.EXTENSION, null);
        piece(a, "Extension séjour", "25", "20", "2.7", TypePiece.SEJOUR, NiveauTravaux.RENOVATION_LOURDE);
        note(a, null, "Visite du 4 : extension de 25 m² côté jardin. Permis déposé par l'architecte, réponse attendue.");
        note(a, null, "Le client souhaite une baie vitrée de 4 m et un sol identique à l'existant (parquet chêne).");
        jdbc.update("UPDATE affaire SET created_at = now() - interval '8 days', updated_at = now() - interval '8 days' WHERE id = ?",
            a.getId());
    }

    private void t2Guillotiere() {
        Affaire a = affaire("T2 Guillotière", "M. Nguyen", null, null, "60 rue de Marseille, 69007 Lyon",
            TypeBien.APPARTEMENT, "Perdu : le client a choisi une offre moins chère.");
        Piece sejour = piece(a, "Séjour", "20", "18", "2.6", TypePiece.SEJOUR, NiveauTravaux.RAFRAICHISSEMENT);
        ligne(a, sejour, "peinture", "65");
        ligne(a, sejour, "parquet", "20");
        affaireService.update(organisation, a.getId(), requete(a, StatutAffaire.PERDUE));
        jdbc.update("UPDATE affaire SET created_at = now() - interval '75 days', updated_at = now() - interval '50 days' WHERE id = ?",
            a.getId());
    }

    // --- Outils ---------------------------------------------------------------------------------

    private Affaire affaire(String nom, String client, String email, String telephone, String adresse, TypeBien type,
            String notes) {
        return affaireService.create(organisation, new AffaireRequest(nom, client, email, telephone, adresse, type, null, notes));
    }

    private AffaireRequest requete(Affaire a, StatutAffaire statut) {
        return new AffaireRequest(a.getNom(), a.getClientNom(), a.getClientEmail(), a.getClientTelephone(), a.getAdresse(),
            a.getTypeBien(), statut, a.getNotes());
    }

    private Piece piece(Affaire a, String nom, String surface, String perimetre, String hauteur, TypePiece type,
            NiveauTravaux niveau) {
        return pieceService.create(organisation, a.getId(), new PieceRequest(nom, dec(surface), dec(perimetre), dec(hauteur),
            type, niveau, null));
    }

    private void note(Affaire a, Piece piece, String texte) {
        visiteService.addObservation(organisation, a.getId(), new ObservationRequest(piece == null ? null : piece.getId(), texte),
            AUTEUR);
    }

    private void ligne(Affaire a, Piece piece, String ouvrage, String quantite) {
        devisService.createFromOuvrage(organisation, a.getId(),
            new LigneDepuisOuvrageRequest(ouvrages.get(ouvrage).getId(), piece == null ? null : piece.getId(), dec(quantite)));
    }

    private void gamme(Affaire a, Piece piece, String designation, String prix, String debourse, Gamme gamme) {
        devisService.create(organisation, a.getId(), new LigneDevisRequest(piece.getId(), lots.get("10").getId(), designation,
            null, "m²", dec("14"), dec(prix), null, gamme, dec(debourse), null));
    }

    private Avenant avenant(Affaire a, String objet, String motif, String[]... lignes) {
        Avenant avenant = avenantService.create(organisation, a.getId(), new AvenantRequest(objet, motif));
        for (String[] l : lignes) {
            Ouvrage o = ouvrages.get(l[0]);
            avenantService.addLigne(organisation, a.getId(), avenant.getId(), new LigneDevisRequest(null, o.getLot().getId(),
                o.getDesignation(), null, o.getUnite(), dec(l[1]), o.getPrixUnitaireHt(), o.getTauxTva(), null,
                o.getDebourseUnitaireHt(), null));
        }
        return avenant;
    }

    private void envoyerEtDecider(Affaire a, Avenant avenant, StatutAvenant decision, String par) {
        avenantService.envoyer(organisation, a.getId(), avenant.getId());
        avenantService.decider(organisation, a.getId(), avenant.getId(), new DecisionRequest(decision, par, "Accord par e-mail"));
    }

    private void depense(Affaire a, String lot, NatureDepense nature, String libelle, String fournisseur, String montant,
            int ilYaJours) {
        margeService.addDepense(organisation, a.getId(), new DepenseRequest(lots.get(lot).getId(), nature, libelle, fournisseur,
            dec(montant), LocalDate.now().minusDays(ilYaJours)));
    }

    /** Recule les dates : création de l'affaire, passage en chantier, décisions d'avenants (null : inchangé). */
    private void dater(Affaire a, int creation, Integer chantier, Integer decisions) {
        UUID id = a.getId();
        jdbc.update("UPDATE affaire SET created_at = now() - make_interval(days => ?), updated_at = now() - make_interval(days => ?) WHERE id = ?",
            creation, Math.min(creation, chantier == null ? creation : chantier), id);
        jdbc.update("UPDATE devis_version SET created_at = now() - make_interval(days => ?) WHERE affaire_id = ? AND numero = 1",
            chantier == null ? creation - 5 : chantier + 7, id);
        if (chantier != null) {
            jdbc.update("""
                UPDATE devis_version SET created_at = now() - make_interval(days => ?)
                WHERE id = (SELECT version_chantier_id FROM affaire WHERE id = ?)""", chantier, id);
        }
        if (decisions != null) {
            jdbc.update("""
                UPDATE avenant SET created_at = now() - make_interval(days => ?), envoye_at = now() - make_interval(days => ?),
                       decide_at = CASE WHEN decide_at IS NULL THEN NULL ELSE now() - make_interval(days => ?) END
                WHERE affaire_id = ? AND statut = 'VALIDE'""", decisions + 5, decisions + 3, decisions, id);
        }
    }

    private static BigDecimal dec(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

}
