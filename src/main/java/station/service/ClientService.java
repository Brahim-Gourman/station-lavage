package station.service;

import station.dao.ClientDao;
import station.dao.LavageDao;
import station.dao.VehiculeDao;
import station.exception.EntiteIntrouvableException;
import station.io.Journal;
import station.model.Client;

import java.util.List;

/** Règles de gestion des clients (F1). */
public class ClientService {

    private final ClientDao clientDao;
    private final VehiculeDao vehiculeDao;
    private final LavageDao lavageDao;

    /** Reçoit les DAO dont il a besoin. */
    public ClientService(ClientDao clientDao, VehiculeDao vehiculeDao, LavageDao lavageDao) {
        this.clientDao = clientDao;
        this.vehiculeDao = vehiculeDao;
        this.lavageDao = lavageDao;
    }

    /** Crée et enregistre un nouveau client. */
    public Client ajouter(String nom, String telephone) {
        Verification.texteNonVide(nom, "nom");
        Verification.texteNonVide(telephone, "téléphone");
        Client client = new Client(0, nom, telephone);
        clientDao.ajouter(client);
        Journal.ecrire("CLIENT", "Ajout de " + client);
        return client;
    }

    /** Change le nom et le téléphone d'un client existant. */
    public void modifier(int id, String nom, String telephone) throws EntiteIntrouvableException {
        Verification.texteNonVide(nom, "nom");
        Verification.texteNonVide(telephone, "téléphone");
        Client client = trouver(id);
        client.setNom(nom);
        client.setTelephone(telephone);
        clientDao.modifier(client);
        Journal.ecrire("CLIENT", "Modification de " + client);
    }

    /** Supprime un client, sauf s'il a déjà des lavages (on garde l'historique). */
    public void supprimer(int id) throws EntiteIntrouvableException {
        Client client = trouver(id);
        boolean aDesLavages = lavageDao.lister().stream()
                .anyMatch(l -> l.getVehicule().getClientId() == id);
        if (aDesLavages) {
            throw new IllegalStateException("Ce client a des lavages : il est gardé pour l'historique.");
        }
        clientDao.supprimer(id);
        Journal.ecrire("CLIENT", "Suppression de " + client);
    }

    /** Retourne le client avec ses véhicules. */
    public Client trouver(int id) throws EntiteIntrouvableException {
        Client client = clientDao.trouverParId(id)
                .orElseThrow(() -> new EntiteIntrouvableException("Client", id));
        vehiculeDao.lister().stream()
                .filter(v -> v.getClientId() == id)
                .forEach(client::ajouterVehicule);
        return client;
    }

    /** Retourne tous les clients. */
    public List<Client> lister() {
        return clientDao.lister();
    }
}
