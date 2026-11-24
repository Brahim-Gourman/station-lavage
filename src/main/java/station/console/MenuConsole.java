package station.console;

import station.service.Services;
import station.simulation.BilanSimulation;

/** Menu principal de la console : affiche les fonctions F1 à F10 et appelle les sous-menus. */
public class MenuConsole {

    private final Services services;
    private final Saisie saisie = new Saisie();
    private final MenuGestion gestion;
    private final MenuLavages lavages;
    private final MenuRapports rapports;

    /** Prépare les sous-menus, qui partagent les mêmes services. */
    public MenuConsole(Services services) {
        this.services = services;
        gestion = new MenuGestion(services, saisie);
        lavages = new MenuLavages(services, saisie);
        rapports = new MenuRapports(services, saisie);
    }

    /** Boucle principale : toute erreur est affichée, le programme continue. */
    public void lancer() {
        while (true) {
            afficherMenu();
            int choix = saisie.lireEntier("Votre choix : ");
            if (choix == 0) {
                System.out.println("Au revoir.");
                return;
            }
            try {
                ouvrir(choix);
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    /** Affiche la liste des fonctions. */
    private void afficherMenu() {
        System.out.println();
        System.out.println("===== STATION DE LAVAGE =====");
        System.out.println(" 1. Clients            2. Véhicules");
        System.out.println(" 3. Laveurs            4. Produits et stock");
        System.out.println(" 5. Enregistrer un lavage");
        System.out.println(" 6. Suivre un lavage");
        System.out.println(" 7. Rechercher         8. Statistiques");
        System.out.println(" 9. Lancer la simulation");
        System.out.println("10. Exporter");
        System.out.println(" 0. Quitter");
    }

    /** F9 : demande le nombre de laveurs et de lavages, lance la simulation et affiche le bilan. */
    private void simuler() throws Exception {
        int nbLaveurs = saisie.lireEntierEntre("Nombre de laveurs (1 à 20) : ", 1, 20);
        int nbLavages = saisie.lireEntierEntre("Nombre de lavages (1 à 500) : ", 1, 500);
        System.out.println("Simulation en cours...");
        BilanSimulation bilan = services.simulation.lancer(nbLaveurs, nbLavages);
        System.out.println(bilan.texte());
        System.out.println(bilan.estCoherent() ? "BILAN JUSTE" : "BILAN INCOHÉRENT");
    }

    /** Lance la fonction choisie. */
    private void ouvrir(int choix) throws Exception {
        switch (choix) {
            case 1 -> gestion.clients();
            case 2 -> gestion.vehicules();
            case 3 -> gestion.laveurs();
            case 4 -> gestion.produits();
            case 5 -> lavages.enregistrer();
            case 6 -> lavages.suivre();
            case 7 -> rapports.rechercher();
            case 8 -> rapports.statistiques();
            case 9 -> simuler();
            case 10 -> rapports.exporter();
            default -> System.out.println("Choix inconnu.");
        }
    }
}
