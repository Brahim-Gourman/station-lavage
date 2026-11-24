package station.service;

import station.dao.LavageDao;
import station.dao.LaveurDao;
import station.exception.EntiteIntrouvableException;
import station.io.Journal;
import station.model.Laveur;

import java.util.List;

/** Règles de gestion des laveurs (F3), avec activation et désactivation. */
public class LaveurService {

    private final LaveurDao laveurDao;
    private final LavageDao lavageDao;

    /** Reçoit les DAO dont il a besoin. */
    public LaveurService(LaveurDao laveurDao, LavageDao lavageDao) {
        this.laveurDao = laveurDao;
        this.lavageDao = lavageDao;
    }

    /** Crée un laveur actif. */
    public Laveur ajouter(String nom) {
        Verification.texteNonVide(nom, "nom");
        Laveur laveur = new Laveur(0, nom, true);
        laveurDao.ajouter(laveur);
        Journal.ecrire("LAVEUR", "Ajout de " + laveur);
        return laveur;
    }

    /** Change le nom d'un laveur. */
    public void modifier(int id, String nom) throws EntiteIntrouvableException {
        Verification.texteNonVide(nom, "nom");
        Laveur laveur = trouver(id);
        laveur.setNom(nom);
        laveurDao.modifier(laveur);
        Journal.ecrire("LAVEUR", "Modification de " + laveur);
    }

    /** Supprime un laveur sans lavage ; sinon il faut le désactiver. */
    public void supprimer(int id) throws EntiteIntrouvableException {
        Laveur laveur = trouver(id);
        boolean aDesLavages = lavageDao.lister().stream()
                .anyMatch(l -> l.getLaveur() != null && l.getLaveur().getId() == id);
        if (aDesLavages) {
            throw new IllegalStateException("Ce laveur a des lavages : désactivez-le plutôt.");
        }
        laveurDao.supprimer(id);
        Journal.ecrire("LAVEUR", "Suppression de " + laveur);
    }

    /** Rend un laveur actif. */
    public void activer(int id) throws EntiteIntrouvableException {
        Laveur laveur = trouver(id);
        laveur.activer();
        laveurDao.modifier(laveur);
        Journal.ecrire("LAVEUR", "Activation de " + laveur);
    }

    /** Désactive un laveur (il quitte la station, son historique reste). */
    public void desactiver(int id) throws EntiteIntrouvableException {
        Laveur laveur = trouver(id);
        laveur.desactiver();
        laveurDao.modifier(laveur);
        Journal.ecrire("LAVEUR", "Désactivation de " + laveur);
    }

    /** Retourne le laveur qui a cet identifiant. */
    public Laveur trouver(int id) throws EntiteIntrouvableException {
        return laveurDao.trouverParId(id)
                .orElseThrow(() -> new EntiteIntrouvableException("Laveur", id));
    }

    /** Retourne tous les laveurs. */
    public List<Laveur> lister() {
        return laveurDao.lister();
    }
}
