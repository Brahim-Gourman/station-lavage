package station.console;

import station.exception.EntiteIntrouvableException;
import station.model.Camion;
import station.model.Client;
import station.model.Moto;
import station.model.Vehicule;
import station.model.Voiture;
import station.service.Services;

/** Sous-menus de gestion : clients (F1), véhicules (F2), laveurs (F3) et produits (F4). */
public class MenuGestion {

    private final Services services;
    private final Saisie saisie;

    /** Reçoit les services et l'outil de saisie partagés. */
    public MenuGestion(Services services, Saisie saisie) {
        this.services = services;
        this.saisie = saisie;
    }

    /** F1 : gérer les clients. */
    public void clients() throws EntiteIntrouvableException {
        System.out.println("1. Ajouter  2. Modifier  3. Supprimer  4. Lister  5. Consulter  0. Retour");
        switch (saisie.lireEntierEntre("Choix : ", 0, 5)) {
            case 1 -> System.out.println("Ajouté : " + services.clients.ajouter(
                    saisie.lireTexte("Nom : "), saisie.lireTexte("Téléphone : ")));
            case 2 -> services.clients.modifier(saisie.lireEntier("Id du client : "),
                    saisie.lireTexte("Nouveau nom : "), saisie.lireTexte("Nouveau téléphone : "));
            case 3 -> services.clients.supprimer(saisie.lireEntier("Id du client : "));
            case 4 -> saisie.afficherListe(services.clients.lister());
            case 5 -> consulterClient();
            default -> { }
        }
    }

    /** F2 : gérer les véhicules. */
    public void vehicules() throws EntiteIntrouvableException {
        System.out.println("1. Ajouter  2. Modifier  3. Supprimer  4. Lister  0. Retour");
        switch (saisie.lireEntierEntre("Choix : ", 0, 4)) {
            case 1 -> ajouterVehicule();
            case 2 -> modifierVehicule();
            case 3 -> services.vehicules.supprimer(saisie.lireEntier("Id du véhicule : "));
            case 4 -> saisie.afficherListe(services.vehicules.lister());
            default -> { }
        }
    }

    /** F3 : gérer les laveurs. */
    public void laveurs() throws EntiteIntrouvableException {
        System.out.println("1. Ajouter  2. Modifier  3. Supprimer  4. Lister  5. Activer  6. Désactiver  0. Retour");
        switch (saisie.lireEntierEntre("Choix : ", 0, 6)) {
            case 1 -> System.out.println("Ajouté : " + services.laveurs.ajouter(saisie.lireTexte("Nom : ")));
            case 2 -> services.laveurs.modifier(saisie.lireEntier("Id du laveur : "),
                    saisie.lireTexte("Nouveau nom : "));
            case 3 -> services.laveurs.supprimer(saisie.lireEntier("Id du laveur : "));
            case 4 -> saisie.afficherListe(services.laveurs.lister());
            case 5 -> services.laveurs.activer(saisie.lireEntier("Id du laveur : "));
            case 6 -> services.laveurs.desactiver(saisie.lireEntier("Id du laveur : "));
            default -> { }
        }
    }

    /** F4 : gérer les produits et réapprovisionner le stock. */
    public void produits() throws EntiteIntrouvableException {
        System.out.println("1. Ajouter  2. Modifier  3. Supprimer  4. Lister  5. Réapprovisionner  0. Retour");
        switch (saisie.lireEntierEntre("Choix : ", 0, 5)) {
            case 1 -> System.out.println("Ajouté : " + services.stock.ajouter(saisie.lireTexte("Nom : "),
                    saisie.lireEntier("Stock (ml) : "), saisie.lireEntier("Seuil d'alerte (ml) : ")));
            case 2 -> services.stock.modifier(saisie.lireEntier("Id du produit : "),
                    saisie.lireTexte("Nouveau nom : "), saisie.lireEntier("Nouveau seuil (ml) : "));
            case 3 -> services.stock.supprimer(saisie.lireEntier("Id du produit : "));
            case 4 -> saisie.afficherListe(services.stock.lister());
            case 5 -> services.stock.reapprovisionner(saisie.lireEntier("Id du produit : "),
                    saisie.lireEntier("Quantité ajoutée (ml) : "));
            default -> { }
        }
    }

    /** Affiche un client et ses véhicules. */
    private void consulterClient() throws EntiteIntrouvableException {
        Client client = services.clients.trouver(saisie.lireEntier("Id du client : "));
        System.out.println(client);
        client.getVehicules().forEach(v -> System.out.println("   " + v));
    }

    /** Demande le type puis les informations du nouveau véhicule. */
    private void ajouterVehicule() throws EntiteIntrouvableException {
        int clientId = saisie.lireEntier("Id du client propriétaire : ");
        int type = saisie.lireEntierEntre("Type (1 Moto, 2 Voiture, 3 Camion) : ", 1, 3);
        String immat = saisie.lireTexte("Immatriculation : ");
        Vehicule vehicule = switch (type) {
            case 1 -> new Moto(0, clientId, immat);
            case 2 -> new Voiture(0, clientId, immat, saisie.lireOuiNon("SUV ?"));
            default -> new Camion(0, clientId, immat, saisie.lireEntierEntre("Nombre d'essieux : ", 2, 10));
        };
        services.vehicules.ajouter(vehicule);
        System.out.println("Ajouté : " + vehicule);
    }

    /** Modifie un véhicule existant ; son type ne change pas. */
    private void modifierVehicule() throws EntiteIntrouvableException {
        Vehicule vehicule = services.vehicules.trouver(saisie.lireEntier("Id du véhicule : "));
        vehicule.setImmatriculation(saisie.lireTexte("Nouvelle immatriculation : "));
        vehicule.setClientId(saisie.lireEntier("Id du client propriétaire : "));
        if (vehicule instanceof Voiture voiture) {
            voiture.setSuv(saisie.lireOuiNon("SUV ?"));
        }
        if (vehicule instanceof Camion camion) {
            camion.setNbEssieux(saisie.lireEntierEntre("Nombre d'essieux : ", 2, 10));
        }
        services.vehicules.modifier(vehicule);
        System.out.println("Modifié : " + vehicule);
    }
}
