package station.service;

import station.dao.LavageDao;
import station.dao.LaveurDao;
import station.dao.VehiculeDao;
import station.exception.EntiteIntrouvableException;
import station.exception.FormuleNonDisponibleException;
import station.exception.StockInsuffisantException;
import station.io.Journal;
import station.model.Formule;
import station.model.Lavage;
import station.model.Laveur;
import station.model.StatutLavage;
import station.model.Vehicule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Création et suivi des lavages (F5 et F6) : prix, fidélité, statuts et stock. */
public class LavageService {

    /** Nombre de lavages terminés à partir duquel un client est fidèle. */
    public static final int SEUIL_FIDELITE = 10;
    /** Remise accordée aux clients fidèles (10 %). */
    public static final double REMISE_FIDELITE = 0.10;

    private final LavageDao lavageDao;
    private final VehiculeDao vehiculeDao;
    private final LaveurDao laveurDao;
    private final StockService stockService;

    /** Reçoit les DAO et le service de stock partagé. */
    public LavageService(LavageDao lavageDao, VehiculeDao vehiculeDao, LaveurDao laveurDao,
                         StockService stockService) {
        this.lavageDao = lavageDao;
        this.vehiculeDao = vehiculeDao;
        this.laveurDao = laveurDao;
        this.stockService = stockService;
    }

    /** Crée un lavage en attente et calcule son montant (remise de fidélité comprise). */
    public Lavage creer(int vehiculeId, Formule formule, Set<Lavage.Option> options)
            throws EntiteIntrouvableException, FormuleNonDisponibleException {
        Vehicule vehicule = vehiculeDao.trouverParId(vehiculeId)
                .orElseThrow(() -> new EntiteIntrouvableException("Véhicule", vehiculeId));
        if (!vehicule.accepte(formule)) {
            throw new FormuleNonDisponibleException("La formule " + formule + " n'est pas possible pour une "
                    + vehicule.getType());
        }
        Lavage lavage = new Lavage(vehicule, formule, calculerRemise(vehicule.getClientId()));
        options.forEach(lavage::ajouterOption);
        lavage.setMontant(lavage.calculerMontant());
        lavageDao.ajouter(lavage);
        Journal.ecrire("LAVAGE", "Création : " + lavage);
        return lavage;
    }

    /** 10 % de remise si le client a au moins 10 lavages terminés, sinon 0. */
    public double calculerRemise(int clientId) {
        long termines = lavageDao.lister().stream()
                .filter(l -> l.getVehicule().getClientId() == clientId)
                .filter(l -> l.getStatut() == StatutLavage.TERMINE)
                .count();
        return termines >= SEUIL_FIDELITE ? REMISE_FIDELITE : 0;
    }

    /** Attribue un laveur actif à un lavage en attente. */
    public void attribuerLaveur(int lavageId, int laveurId) throws EntiteIntrouvableException {
        Lavage lavage = trouver(lavageId);
        verifierStatut(lavage, StatutLavage.EN_ATTENTE);
        Laveur laveur = laveurDao.trouverParId(laveurId)
                .orElseThrow(() -> new EntiteIntrouvableException("Laveur", laveurId));
        if (!laveur.isActif()) {
            throw new IllegalStateException("Le laveur " + laveur.getNom() + " est inactif.");
        }
        lavage.setLaveur(laveur);
        lavageDao.modifier(lavage);
        Journal.ecrire("LAVAGE", "Laveur attribué : " + lavage);
    }

    /** Démarre un lavage : retire les produits ; si le stock manque, le lavage est annulé. */
    public void demarrer(int lavageId) throws EntiteIntrouvableException, StockInsuffisantException {
        Lavage lavage = trouver(lavageId);
        verifierStatut(lavage, StatutLavage.EN_ATTENTE);
        verifierLaveurLibre(lavage);
        try {
            stockService.consommer(lavage.getFormule());
        } catch (StockInsuffisantException e) {
            changerStatut(lavage, StatutLavage.ANNULE, e.getMessage());
            throw e;
        }
        changerStatut(lavage, StatutLavage.EN_COURS, "Démarrage");
    }

    /** Termine un lavage en cours et enregistre la date de fin. */
    public void terminer(int lavageId) throws EntiteIntrouvableException {
        Lavage lavage = trouver(lavageId);
        verifierStatut(lavage, StatutLavage.EN_COURS);
        lavage.setDateFin(LocalDateTime.now());
        changerStatut(lavage, StatutLavage.TERMINE, "Fin");
    }

    /** Annule un lavage qui n'a pas encore commencé. */
    public void annuler(int lavageId) throws EntiteIntrouvableException {
        Lavage lavage = trouver(lavageId);
        verifierStatut(lavage, StatutLavage.EN_ATTENTE);
        changerStatut(lavage, StatutLavage.ANNULE, "Annulation");
    }

    /** Retourne le lavage qui a cet identifiant. */
    public Lavage trouver(int id) throws EntiteIntrouvableException {
        return lavageDao.trouverParId(id)
                .orElseThrow(() -> new EntiteIntrouvableException("Lavage", id));
    }

    /** Retourne tous les lavages (List : on garde l'ordre de création). */
    public List<Lavage> lister() {
        return lavageDao.lister();
    }

    /** Refuse l'action si le lavage n'a pas le statut attendu. */
    private void verifierStatut(Lavage lavage, StatutLavage attendu) {
        if (lavage.getStatut() != attendu) {
            throw new IllegalStateException("Action impossible : le lavage est " + lavage.getStatut()
                    + " (il faut " + attendu + ").");
        }
    }

    /** Un lavage démarre seulement avec un laveur qui n'a pas déjà un lavage en cours. */
    private void verifierLaveurLibre(Lavage lavage) {
        if (lavage.getLaveur() == null) {
            throw new IllegalStateException("Attribuez d'abord un laveur à ce lavage.");
        }
        int laveurId = lavage.getLaveur().getId();
        boolean occupe = lavageDao.lister().stream()
                .anyMatch(l -> l.getStatut() == StatutLavage.EN_COURS
                        && l.getLaveur() != null && l.getLaveur().getId() == laveurId);
        if (occupe) {
            throw new IllegalStateException("Ce laveur a déjà un lavage en cours.");
        }
    }

    /** Change le statut, enregistre en base et écrit dans le journal. */
    private void changerStatut(Lavage lavage, StatutLavage statut, String raison) {
        lavage.setStatut(statut);
        lavageDao.modifier(lavage);
        Journal.ecrire("LAVAGE", raison + " : " + lavage);
    }
}
