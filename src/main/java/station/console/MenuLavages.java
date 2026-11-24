package station.console;

import station.exception.EntiteIntrouvableException;
import station.exception.FormuleNonDisponibleException;
import station.exception.StockInsuffisantException;
import station.model.Formule;
import station.model.Lavage;
import station.model.StatutLavage;
import station.service.Services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Sous-menus des lavages : enregistrer (F5) et suivre (F6). */
public class MenuLavages {

    private final Services services;
    private final Saisie saisie;

    /** Reçoit les services et l'outil de saisie partagés. */
    public MenuLavages(Services services, Saisie saisie) {
        this.services = services;
        this.saisie = saisie;
    }

    /** F5 : choisir le véhicule, la formule et les options, puis afficher le montant. */
    public void enregistrer() throws EntiteIntrouvableException, FormuleNonDisponibleException {
        saisie.afficherListe(services.vehicules.lister());
        int vehiculeId = saisie.lireEntier("Id du véhicule : ");
        Formule formule = choisirFormule();
        Set<Lavage.Option> options = choisirOptions();
        Lavage lavage = services.lavages.creer(vehiculeId, formule, options);
        System.out.println("Lavage créé : " + lavage);
        if (lavage.getRemise() > 0) {
            System.out.println("Client fidèle : remise de 10 % appliquée.");
        }
    }

    /** F6 : attribuer un laveur, démarrer, terminer ou annuler un lavage. */
    public void suivre() throws EntiteIntrouvableException, StockInsuffisantException {
        System.out.println("En attente :");
        saisie.afficherListe(services.recherche.lavagesParStatut(StatutLavage.EN_ATTENTE));
        System.out.println("En cours :");
        saisie.afficherListe(services.recherche.lavagesParStatut(StatutLavage.EN_COURS));
        System.out.println("1. Attribuer un laveur  2. Démarrer  3. Terminer  4. Annuler  0. Retour");
        int choix = saisie.lireEntierEntre("Choix : ", 0, 4);
        if (choix == 0) {
            return;
        }
        int lavageId = saisie.lireEntier("Id du lavage : ");
        switch (choix) {
            case 1 -> attribuer(lavageId);
            case 2 -> services.lavages.demarrer(lavageId);
            case 3 -> services.lavages.terminer(lavageId);
            default -> services.lavages.annuler(lavageId);
        }
        System.out.println("Fait : " + services.lavages.trouver(lavageId));
    }

    /** Affiche les laveurs puis attribue celui choisi. */
    private void attribuer(int lavageId) throws EntiteIntrouvableException {
        saisie.afficherListe(services.laveurs.lister());
        services.lavages.attribuerLaveur(lavageId, saisie.lireEntier("Id du laveur : "));
    }

    /** Demande une formule parmi les trois. */
    private Formule choisirFormule() {
        Formule[] formules = Formule.values();
        for (int i = 0; i < formules.length; i++) {
            System.out.println((i + 1) + ". " + formules[i] + " (" + formules[i].getPrixBase() + " MAD)");
        }
        return formules[saisie.lireEntierEntre("Formule : ", 1, formules.length) - 1];
    }

    /** Demande les options une par une ; le Set empêche de prendre deux fois la même. */
    private Set<Lavage.Option> choisirOptions() {
        List<Lavage.Option> catalogue = Lavage.Option.catalogue();
        Set<Lavage.Option> choisies = new HashSet<>();
        for (int i = 0; i < catalogue.size(); i++) {
            System.out.println((i + 1) + ". " + catalogue.get(i));
        }
        int choix = saisie.lireEntierEntre("Option (0 pour finir) : ", 0, catalogue.size());
        while (choix != 0) {
            if (!choisies.add(catalogue.get(choix - 1))) {
                System.out.println("Option déjà choisie.");
            }
            choix = saisie.lireEntierEntre("Option (0 pour finir) : ", 0, catalogue.size());
        }
        return choisies;
    }
}
