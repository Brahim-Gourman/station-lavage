package station.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import station.service.Services;

/** Point d'entrée de la version JavaFX : une fenêtre avec trois onglets. */
public class ApplicationFx extends Application {

    /** Crée les services (les mêmes que la console) et les trois écrans. */
    @Override
    public void start(Stage fenetre) {
        Services services = new Services();
        ClientsController clients = new ClientsController(services);
        LavagesController lavages = new LavagesController(services);
        TableauBordController tableau = new TableauBordController(services);
        Tab ongletClients = new Tab("Clients et véhicules", clients.getVue());
        Tab ongletLavages = new Tab("Lavages", lavages.getVue());
        Tab ongletTableau = new Tab("Tableau de bord", tableau.getVue());
        ongletClients.setOnSelectionChanged(e -> clients.rafraichir());
        ongletLavages.setOnSelectionChanged(e -> lavages.rafraichir());
        ongletTableau.setOnSelectionChanged(e -> tableau.rafraichir());
        TabPane onglets = new TabPane(ongletClients, ongletLavages, ongletTableau);
        onglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        fenetre.setTitle("Station de lavage");
        fenetre.setScene(new Scene(onglets, 1000, 650));
        fenetre.show();
    }

    /** Lance JavaFX. */
    public static void main(String[] args) {
        launch(args);
    }
}
