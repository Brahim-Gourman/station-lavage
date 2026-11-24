package station.service;

import station.dao.ClientDao;
import station.dao.LavageDao;
import station.dao.VehiculeDao;
import station.exception.EntiteIntrouvableException;
import station.io.Journal;
import station.model.Vehicule;

import java.util.List;

/** Règles de gestion des véhicules (F2). Chaque véhicule appartient à un client. */
public class VehiculeService {

    private final VehiculeDao vehiculeDao;
    private final ClientDao clientDao;
    private final LavageDao lavageDao;

    /** Reçoit les DAO dont il a besoin. */
    public VehiculeService(VehiculeDao vehiculeDao, ClientDao clientDao, LavageDao lavageDao) {
        this.vehiculeDao = vehiculeDao;
        this.clientDao = clientDao;
        this.lavageDao = lavageDao;
    }

    /** Enregistre un véhicule si son client existe. */
    public void ajouter(Vehicule vehicule) throws EntiteIntrouvableException {
        Verification.texteNonVide(vehicule.getImmatriculation(), "immatriculation");
        verifierClient(vehicule.getClientId());
        vehiculeDao.ajouter(vehicule);
        Journal.ecrire("VEHICULE", "Ajout de " + vehicule);
    }

    /** Enregistre les changements d'un véhicule existant. */
    public void modifier(Vehicule vehicule) throws EntiteIntrouvableException {
        Verification.texteNonVide(vehicule.getImmatriculation(), "immatriculation");
        trouver(vehicule.getId());
        verifierClient(vehicule.getClientId());
        vehiculeDao.modifier(vehicule);
        Journal.ecrire("VEHICULE", "Modification de " + vehicule);
    }

    /** Supprime un véhicule, sauf s'il a déjà des lavages. */
    public void supprimer(int id) throws EntiteIntrouvableException {
        Vehicule vehicule = trouver(id);
        boolean aDesLavages = lavageDao.lister().stream()
                .anyMatch(l -> l.getVehicule().getId() == id);
        if (aDesLavages) {
            throw new IllegalStateException("Ce véhicule a des lavages : il est gardé pour l'historique.");
        }
        vehiculeDao.supprimer(id);
        Journal.ecrire("VEHICULE", "Suppression de " + vehicule);
    }

    /** Retourne le véhicule qui a cet identifiant. */
    public Vehicule trouver(int id) throws EntiteIntrouvableException {
        return vehiculeDao.trouverParId(id)
                .orElseThrow(() -> new EntiteIntrouvableException("Véhicule", id));
    }

    /** Retourne tous les véhicules. */
    public List<Vehicule> lister() {
        return vehiculeDao.lister();
    }

    /** Lève une exception si le client n'existe pas. */
    private void verifierClient(int clientId) throws EntiteIntrouvableException {
        if (clientDao.trouverParId(clientId).isEmpty()) {
            throw new EntiteIntrouvableException("Client", clientId);
        }
    }
}
