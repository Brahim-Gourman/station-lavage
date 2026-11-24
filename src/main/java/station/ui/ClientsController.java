package station.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import station.exception.EntiteIntrouvableException;
import station.model.Camion;
import station.model.Client;
import station.model.Moto;
import station.model.Vehicule;
import station.model.Voiture;
import station.service.Services;

/** Écran "Clients et véhicules" : ajouter, modifier, supprimer, rechercher (appelle ClientService et VehiculeService). */
public class ClientsController {

    private final Services services;
    private final ListView<Client> listeClients = new ListView<>();
    private final ListView<Vehicule> listeVehicules = new ListView<>();
    private final TextField champRecherche = new TextField();
    private final TextField champNom = new TextField();
    private final TextField champTelephone = new TextField();
    private final ComboBox<String> choixType = new ComboBox<>();
    private final TextField champImmatriculation = new TextField();
    private final CheckBox caseSuv = new CheckBox("SUV");
    private final Spinner<Integer> essieux = new Spinner<>(2, 10, 2);
    private final HBox vue;

    /** Construit l'écran et branche les sélections. */
    public ClientsController(Services services) {
        this.services = services;
        choixType.getItems().addAll("MOTO", "VOITURE", "CAMION");
        choixType.setValue("VOITURE");
        listeClients.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, client) -> OutilsFx.essayer(() -> choisirClient(client)));
        listeVehicules.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, vehicule) -> choisirVehicule(vehicule));
        vue = new HBox(20, colonneClients(), colonneVehicules());
        vue.setPadding(new Insets(15));
        rafraichir();
    }

    /** Racine graphique de l'écran. */
    public Parent getVue() {
        return vue;
    }

    /** Recharge la liste des clients depuis le service. */
    public void rafraichir() {
        listeClients.getItems().setAll(services.clients.lister());
        listeVehicules.getItems().clear();
    }

    /** Colonne de gauche : recherche, liste et formulaire des clients. */
    private VBox colonneClients() {
        champRecherche.setPromptText("Nom ou téléphone");
        champNom.setPromptText("Nom");
        champTelephone.setPromptText("Téléphone");
        HBox recherche = new HBox(5, champRecherche, OutilsFx.bouton("Rechercher", this::rechercher));
        HBox boutons = new HBox(5, OutilsFx.bouton("Ajouter", this::ajouterClient),
                OutilsFx.bouton("Modifier", this::modifierClient),
                OutilsFx.bouton("Supprimer", this::supprimerClient));
        return new VBox(8, OutilsFx.titre("Clients"), recherche, listeClients,
                champNom, champTelephone, boutons);
    }

    /** Colonne de droite : véhicules du client choisi et leur formulaire. */
    private VBox colonneVehicules() {
        champImmatriculation.setPromptText("Immatriculation");
        HBox details = new HBox(8, choixType, caseSuv, new Label("Essieux :"), essieux);
        HBox boutons = new HBox(5, OutilsFx.bouton("Ajouter", this::ajouterVehicule),
                OutilsFx.bouton("Modifier", this::modifierVehicule),
                OutilsFx.bouton("Supprimer", this::supprimerVehicule));
        return new VBox(8, OutilsFx.titre("Véhicules du client choisi"), listeVehicules,
                champImmatriculation, details, boutons);
    }

    /** Recherche par téléphone si le texte commence par un chiffre, sinon par nom. */
    private void rechercher() {
        String texte = champRecherche.getText().trim();
        if (texte.isEmpty()) {
            rafraichir();
        } else if (Character.isDigit(texte.charAt(0))) {
            listeClients.getItems().setAll(services.recherche.clientsParTelephone(texte));
        } else {
            listeClients.getItems().setAll(services.recherche.clientsParNom(texte));
        }
    }

    /** Remplit le formulaire et affiche les véhicules du client sélectionné. */
    private void choisirClient(Client client) throws EntiteIntrouvableException {
        if (client == null) {
            return;
        }
        champNom.setText(client.getNom());
        champTelephone.setText(client.getTelephone());
        listeVehicules.getItems().setAll(services.clients.trouver(client.getId()).getVehicules());
    }

    /** Remplit le formulaire avec le véhicule sélectionné. */
    private void choisirVehicule(Vehicule vehicule) {
        if (vehicule == null) {
            return;
        }
        champImmatriculation.setText(vehicule.getImmatriculation());
        choixType.setValue(vehicule.getType());
        caseSuv.setSelected(vehicule instanceof Voiture v && v.isSuv());
        essieux.getValueFactory().setValue(vehicule instanceof Camion c ? c.getNbEssieux() : 2);
    }

    /** Bouton Ajouter (client). */
    private void ajouterClient() {
        services.clients.ajouter(champNom.getText(), champTelephone.getText());
        rafraichir();
    }

    /** Bouton Modifier (client). */
    private void modifierClient() throws EntiteIntrouvableException {
        services.clients.modifier(clientChoisi().getId(), champNom.getText(), champTelephone.getText());
        rafraichir();
    }

    /** Bouton Supprimer (client). */
    private void supprimerClient() throws EntiteIntrouvableException {
        services.clients.supprimer(clientChoisi().getId());
        rafraichir();
    }

    /** Bouton Ajouter (véhicule) : crée le bon type de véhicule pour le client choisi. */
    private void ajouterVehicule() throws EntiteIntrouvableException {
        Client client = clientChoisi();
        String immat = champImmatriculation.getText().trim();
        Vehicule vehicule = switch (choixType.getValue()) {
            case "MOTO" -> new Moto(0, client.getId(), immat);
            case "VOITURE" -> new Voiture(0, client.getId(), immat, caseSuv.isSelected());
            default -> new Camion(0, client.getId(), immat, essieux.getValue());
        };
        services.vehicules.ajouter(vehicule);
        choisirClient(client);
    }

    /** Bouton Modifier (véhicule) : le type ne change pas. */
    private void modifierVehicule() throws EntiteIntrouvableException {
        Vehicule vehicule = vehiculeChoisi();
        vehicule.setImmatriculation(champImmatriculation.getText().trim());
        if (vehicule instanceof Voiture voiture) {
            voiture.setSuv(caseSuv.isSelected());
        }
        if (vehicule instanceof Camion camion) {
            camion.setNbEssieux(essieux.getValue());
        }
        services.vehicules.modifier(vehicule);
        choisirClient(clientChoisi());
    }

    /** Bouton Supprimer (véhicule). */
    private void supprimerVehicule() throws EntiteIntrouvableException {
        services.vehicules.supprimer(vehiculeChoisi().getId());
        choisirClient(clientChoisi());
    }

    /** Client sélectionné, ou erreur claire s'il n'y en a pas. */
    private Client clientChoisi() {
        Client client = listeClients.getSelectionModel().getSelectedItem();
        if (client == null) {
            throw new IllegalStateException("Sélectionnez d'abord un client.");
        }
        return client;
    }

    /** Véhicule sélectionné, ou erreur claire s'il n'y en a pas. */
    private Vehicule vehiculeChoisi() {
        Vehicule vehicule = listeVehicules.getSelectionModel().getSelectedItem();
        if (vehicule == null) {
            throw new IllegalStateException("Sélectionnez d'abord un véhicule.");
        }
        return vehicule;
    }
}
