package station.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import station.service.Services;
import station.service.StatistiqueService;
import station.simulation.BilanSimulation;

/** Écran "Tableau de bord" : statistiques et simulation (appelle StatistiqueService et SimulationService). */
public class TableauBordController {

    private final Services services;
    private final TextArea zoneStatistiques = new TextArea();
    private final TextArea zoneBilan = new TextArea();
    private final Spinner<Integer> nbLaveurs = new Spinner<>(1, 20, 4);
    private final Spinner<Integer> nbLavages = new Spinner<>(1, 500, 30);
    private final Button boutonLancer = OutilsFx.bouton("Lancer la simulation", this::lancerSimulation);
    private final ProgressIndicator attente = new ProgressIndicator();
    private final HBox vue;

    /** Construit l'écran. */
    public TableauBordController(Services services) {
        this.services = services;
        zoneStatistiques.setEditable(false);
        zoneStatistiques.setWrapText(true);
        zoneBilan.setEditable(false);
        attente.setVisible(false);
        attente.setPrefSize(30, 30);
        VBox statistiques = new VBox(8, OutilsFx.titre("Statistiques"), zoneStatistiques,
                OutilsFx.bouton("Actualiser", this::rafraichir));
        HBox reglages = new HBox(8, new Label("Laveurs :"), nbLaveurs, new Label("Lavages :"), nbLavages);
        VBox simulation = new VBox(8, OutilsFx.titre("Simulation"), reglages,
                new HBox(8, boutonLancer, attente), zoneBilan);
        vue = new HBox(20, statistiques, simulation);
        vue.setPadding(new Insets(15));
        rafraichir();
    }

    /** Racine graphique de l'écran. */
    public Parent getVue() {
        return vue;
    }

    /** Recalcule et affiche les statistiques. */
    public void rafraichir() {
        StatistiqueService s = services.statistiques;
        zoneStatistiques.setText("Chiffre d'affaires total : " + s.caTotal() + " MAD\n"
                + "CA par type de véhicule : " + s.caParTypeVehicule() + "\n"
                + "CA par formule : " + s.caParFormule() + "\n"
                + "Lavages par laveur : " + s.lavagesParLaveur() + "\n"
                + "Meilleur laveur : " + s.meilleurLaveur().orElse("aucun") + "\n"
                + "Formule la plus demandée : " + s.formuleLaPlusDemandee().map(Enum::name).orElse("aucune") + "\n"
                + String.format("Panier moyen : %.2f MAD%n", s.panierMoyen())
                + "Produits sous le seuil : " + s.produitsSousSeuil());
    }

    /**
     * Lance la simulation dans un autre thread : le thread JavaFX reste libre
     * et la fenêtre ne se bloque pas pendant le calcul.
     */
    private void lancerSimulation() {
        int laveurs = nbLaveurs.getValue();
        int lavages = nbLavages.getValue();
        boutonLancer.setDisable(true);
        attente.setVisible(true);
        zoneBilan.setText("Simulation en cours...");
        Thread fil = new Thread(() -> simulerHorsInterface(laveurs, lavages));
        fil.setDaemon(true);
        fil.start();
    }

    /** Code exécuté hors du thread JavaFX ; l'affichage repasse par Platform.runLater(). */
    private void simulerHorsInterface(int laveurs, int lavages) {
        try {
            BilanSimulation bilan = services.simulation.lancer(laveurs, lavages);
            String verdict = bilan.estCoherent() ? "\nBILAN JUSTE" : "\nBILAN INCOHÉRENT";
            Platform.runLater(() -> finirSimulation(bilan.texte() + verdict));
        } catch (Exception e) {
            Platform.runLater(() -> finirSimulation("Erreur : " + e.getMessage()));
        }
    }

    /** Exécuté sur le thread JavaFX : affiche le bilan et réactive le bouton. */
    private void finirSimulation(String texte) {
        zoneBilan.setText(texte);
        boutonLancer.setDisable(false);
        attente.setVisible(false);
        rafraichir();
    }
}
