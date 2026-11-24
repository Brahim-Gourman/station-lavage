package station.console;

import station.exception.EntiteIntrouvableException;
import station.io.Journal;
import station.model.StatutLavage;
import station.service.RechercheService;
import station.service.Services;
import station.service.StatistiqueService;

import java.io.IOException;

/** Sous-menus de consultation : recherches (F7), statistiques (F8) et exports (F10). */
public class MenuRapports {

    private final Services services;
    private final Saisie saisie;

    /** Reçoit les services et l'outil de saisie partagés. */
    public MenuRapports(Services services, Saisie saisie) {
        this.services = services;
        this.saisie = saisie;
    }

    /** F7 : les six recherches. */
    public void rechercher() {
        System.out.println("1. Client par nom  2. Client par téléphone  3. Véhicule par immatriculation");
        System.out.println("4. Lavages par date  5. Lavages par statut  6. Lavages par laveur  0. Retour");
        RechercheService r = services.recherche;
        switch (saisie.lireEntierEntre("Choix : ", 0, 6)) {
            case 1 -> saisie.afficherListe(r.clientsParNom(saisie.lireTexte("Nom (ou partie) : ")));
            case 2 -> saisie.afficherListe(r.clientsParTelephone(saisie.lireTexte("Téléphone (ou partie) : ")));
            case 3 -> System.out.println(r.vehiculeParImmatriculation(saisie.lireTexte("Immatriculation : "))
                    .map(Object::toString).orElse("(aucun résultat)"));
            case 4 -> saisie.afficherListe(r.lavagesParDate(saisie.lireDate("Date (aaaa-mm-jj) : ")));
            case 5 -> saisie.afficherListe(r.lavagesParStatut(choisirStatut()));
            case 6 -> saisie.afficherListe(r.lavagesParLaveur(saisie.lireEntier("Id du laveur : ")));
            default -> { }
        }
    }

    /** F8 : affiche toutes les statistiques. */
    public void statistiques() {
        StatistiqueService s = services.statistiques;
        System.out.println("Chiffre d'affaires total : " + s.caTotal() + " MAD");
        System.out.println("CA par type de véhicule  : " + s.caParTypeVehicule());
        System.out.println("CA par formule           : " + s.caParFormule());
        System.out.println("Lavages par laveur       : " + s.lavagesParLaveur());
        System.out.println("Meilleur laveur          : " + s.meilleurLaveur().orElse("aucun"));
        System.out.println("Formule la plus demandée : "
                + s.formuleLaPlusDemandee().map(Enum::name).orElse("aucune"));
        System.out.printf("Panier moyen             : %.2f MAD%n", s.panierMoyen());
        System.out.println("Produits sous le seuil   :");
        saisie.afficherListe(s.produitsSousSeuil());
    }

    /** F10 : export CSV, ticket .txt et lecture du journal. */
    public void exporter() throws IOException, EntiteIntrouvableException {
        System.out.println("1. Lavages en CSV  2. Ticket d'un lavage  3. Afficher le journal  0. Retour");
        switch (saisie.lireEntierEntre("Choix : ", 0, 3)) {
            case 1 -> System.out.println("Fichier créé : "
                    + services.export.exporterLavagesCsv(services.lavages.lister()));
            case 2 -> System.out.println("Fichier créé : " + services.export.genererTicket(
                    services.lavages.trouver(saisie.lireEntier("Id du lavage : "))));
            case 3 -> saisie.afficherListe(Journal.lire());
            default -> { }
        }
    }

    /** Demande un statut parmi les quatre. */
    private StatutLavage choisirStatut() {
        StatutLavage[] statuts = StatutLavage.values();
        System.out.println("1. EN_ATTENTE  2. EN_COURS  3. TERMINE  4. ANNULE");
        return statuts[saisie.lireEntierEntre("Statut : ", 1, statuts.length) - 1];
    }
}
