package station.service;

import station.dao.ClientDao;
import station.dao.LavageDao;
import station.dao.VehiculeDao;
import station.model.Client;
import station.model.Lavage;
import station.model.StatutLavage;
import station.model.Vehicule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Recherches (F7), toutes écrites avec l'API Stream, sans boucle. */
public class RechercheService {

    private final ClientDao clientDao;
    private final VehiculeDao vehiculeDao;
    private final LavageDao lavageDao;

    /** Reçoit les DAO dont il a besoin. */
    public RechercheService(ClientDao clientDao, VehiculeDao vehiculeDao, LavageDao lavageDao) {
        this.clientDao = clientDao;
        this.vehiculeDao = vehiculeDao;
        this.lavageDao = lavageDao;
    }

    /** Clients dont le nom contient le texte cherché (sans tenir compte des majuscules). */
    public List<Client> clientsParNom(String texte) {
        String cherche = texte.toLowerCase();
        return clientDao.lister().stream()
                .filter(c -> c.getNom().toLowerCase().contains(cherche))
                .toList();
    }

    /** Clients dont le téléphone contient les chiffres cherchés. */
    public List<Client> clientsParTelephone(String texte) {
        return clientDao.lister().stream()
                .filter(c -> c.getTelephone().contains(texte))
                .toList();
    }

    /** Véhicule qui a exactement cette immatriculation (sans tenir compte des majuscules). */
    public Optional<Vehicule> vehiculeParImmatriculation(String immatriculation) {
        return vehiculeDao.lister().stream()
                .filter(v -> v.getImmatriculation().equalsIgnoreCase(immatriculation))
                .findFirst();
    }

    /** Lavages créés un jour donné. */
    public List<Lavage> lavagesParDate(LocalDate jour) {
        return lavageDao.lister().stream()
                .filter(l -> l.getDateCreation().toLocalDate().equals(jour))
                .toList();
    }

    /** Lavages qui ont un statut donné (ex. la file d'attente = EN_ATTENTE). */
    public List<Lavage> lavagesParStatut(StatutLavage statut) {
        return lavageDao.lister().stream()
                .filter(l -> l.getStatut() == statut)
                .toList();
    }

    /** Lavages attribués à un laveur. */
    public List<Lavage> lavagesParLaveur(int laveurId) {
        return lavageDao.lister().stream()
                .filter(l -> l.getLaveur() != null && l.getLaveur().getId() == laveurId)
                .toList();
    }
}
