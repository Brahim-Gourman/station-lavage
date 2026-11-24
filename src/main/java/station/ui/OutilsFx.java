package station.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Petits outils partagés par les trois écrans : boutons protégés et messages. */
public class OutilsFx {

    /** Action lancée par un bouton ; elle peut lever une exception (métier ou base). */
    public interface Action {
        /** Le travail à faire quand on clique. */
        void executer() throws Exception;
    }

    /** Crée un bouton dont l'action affiche une fenêtre d'erreur au lieu de planter. */
    public static Button bouton(String texte, Action action) {
        Button bouton = new Button(texte);
        bouton.setOnAction(e -> essayer(action));
        return bouton;
    }

    /** Exécute l'action ; en cas d'exception, affiche son message. */
    public static void essayer(Action action) {
        try {
            action.executer();
        } catch (Exception e) {
            Alert alerte = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alerte.setHeaderText("Opération impossible");
            alerte.showAndWait();
        }
    }

    /** Affiche une information. */
    public static void info(String message) {
        Alert alerte = new Alert(Alert.AlertType.INFORMATION, message);
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }

    /** Crée un titre de section en gras. */
    public static Label titre(String texte) {
        Label label = new Label(texte);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        return label;
    }
}
