package station.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import station.model.Formule;
import station.model.Lavage;
import station.model.Laveur;
import station.model.StatutLavage;
import station.model.Vehicule;
import station.service.Services;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Écran "Lavages" : créer un lavage, voir la file d'attente, suivre les statuts (appelle LavageService). */
public class LavagesController {

    private static final String TOUS = "TOUS";

    private final Services services;
    private final ComboBox<Vehicule> choixVehicule = new ComboBox<>();
    private final ComboBox<Formule> choixFormule = new ComboBox<>();
    private final List<CheckBox> casesOptions;
    private final Label resultat = new Label();
    private final ComboBox<String> filtre = new ComboBox<>();
    private final ListView<Lavage> listeLavages = new ListView<>();
    private final ComboBox<Laveur> choixLaveur = new ComboBox<>();
    private final HBox vue;

    /** Construit l'écran. Une case à cocher par option du catalogue. */
    public LavagesController(Services services) {
        this.services = services;
        casesOptions = Lavage.Option.catalogue().stream().map(this::caseOption).toList();
        choixFormule.getItems().addAll(Formule.values());
        choixFormule.setValue(Formule.EXPRESS);
        filtre.getItems().add(TOUS);
        filtre.getItems().addAll(Arrays.stream(StatutLavage.values()).map(Enum::name).toList());
        filtre.setValue(StatutLavage.EN_ATTENTE.name());
        filtre.setOnAction(e -> afficherLavages());
        vue = new HBox(20, colonneCreation(), colonneSuivi());
        vue.setPadding(new Insets(15));
        rafraichir();
    }

    /** Racine graphique de l'écran. */
    public Parent getVue() {
        return vue;
    }

    /** Recharge les véhicules, les laveurs et les lavages. */
    public void rafraichir() {
        choixVehicule.getItems().setAll(services.vehicules.lister());
        choixLaveur.getItems().setAll(services.laveurs.lister());
        afficherLavages();
    }

    /** Colonne de gauche : formulaire de création (F5). */
    private VBox colonneCreation() {
        choixVehicule.setPromptText("Véhicule");
        VBox options = new VBox(5);
        options.getChildren().addAll(casesOptions);
        return new VBox(8, OutilsFx.titre("Nouveau lavage"), new Label("Véhicule :"), choixVehicule,
                new Label("Formule :"), choixFormule, new Label("Options :"), options,
                OutilsFx.bouton("Créer le lavage", this::creer), resultat);
    }

    /** Colonne de droite : liste filtrée et boutons de suivi (F6). */
    private VBox colonneSuivi() {
        choixLaveur.setPromptText("Laveur");
        HBox ligneFiltre = new HBox(8, new Label("Statut :"), filtre);
        HBox boutons = new HBox(5, choixLaveur, boutonSuivi("Attribuer", this::attribuer),
                boutonSuivi("Démarrer", () -> services.lavages.demarrer(lavageChoisi().getId())),
                boutonSuivi("Terminer", () -> services.lavages.terminer(lavageChoisi().getId())),
                boutonSuivi("Annuler", () -> services.lavages.annuler(lavageChoisi().getId())),
                OutilsFx.bouton("Ticket", this::ticket));
        listeLavages.setPrefWidth(620);
        return new VBox(8, OutilsFx.titre("Suivi des lavages"), ligneFiltre, listeLavages, boutons);
    }

    /** Bouton de suivi : la liste est rechargée après l'action, même si elle a échoué (ex. lavage annulé faute de stock). */
    private Button boutonSuivi(String texte, OutilsFx.Action action) {
        return OutilsFx.bouton(texte, () -> {
            try {
                action.executer();
            } finally {
                afficherLavages();
            }
        });
    }

    /** Crée une case à cocher qui garde son option dans ses données. */
    private CheckBox caseOption(Lavage.Option option) {
        CheckBox caseACocher = new CheckBox(option.toString());
        caseACocher.setUserData(option);
        return caseACocher;
    }

    /** Affiche tous les lavages, ou seulement ceux du statut choisi. */
    private void afficherLavages() {
        if (TOUS.equals(filtre.getValue())) {
            listeLavages.getItems().setAll(services.lavages.lister());
        } else {
            StatutLavage statut = StatutLavage.valueOf(filtre.getValue());
            listeLavages.getItems().setAll(services.recherche.lavagesParStatut(statut));
        }
    }

    /** Bouton Créer : le prix est calculé par le service, pas par l'écran. */
    private void creer() throws Exception {
        if (choixVehicule.getValue() == null) {
            throw new IllegalStateException("Choisissez un véhicule.");
        }
        Set<Lavage.Option> options = casesOptions.stream()
                .filter(CheckBox::isSelected)
                .map(c -> (Lavage.Option) c.getUserData())
                .collect(Collectors.toSet());
        Lavage lavage = services.lavages.creer(choixVehicule.getValue().getId(), choixFormule.getValue(), options);
        String remise = lavage.getRemise() > 0 ? " (remise fidélité 10 %)" : "";
        resultat.setText("Lavage " + lavage.getId() + " créé : " + lavage.getMontant() + " MAD" + remise);
        afficherLavages();
    }

    /** Bouton Attribuer : donne le laveur choisi au lavage choisi. */
    private void attribuer() throws Exception {
        if (choixLaveur.getValue() == null) {
            throw new IllegalStateException("Choisissez un laveur.");
        }
        services.lavages.attribuerLaveur(lavageChoisi().getId(), choixLaveur.getValue().getId());
    }

    /** Bouton Ticket : écrit le fichier .txt du lavage choisi. */
    private void ticket() throws Exception {
        OutilsFx.info("Ticket créé : " + services.export.genererTicket(lavageChoisi()).toAbsolutePath());
    }

    /** Lavage sélectionné, ou erreur claire s'il n'y en a pas. */
    private Lavage lavageChoisi() {
        Lavage lavage = listeLavages.getSelectionModel().getSelectedItem();
        if (lavage == null) {
            throw new IllegalStateException("Sélectionnez d'abord un lavage.");
        }
        return lavage;
    }
}
