package station.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Ouvre les connexions JDBC vers la base H2 stockée dans le fichier data/station. */
public class Connexion {

    /** DB_CLOSE_DELAY=-1 : la base reste ouverte tant que le programme tourne, même entre deux connexions. */
    private static final String URL = "jdbc:h2:./data/station;DB_CLOSE_DELAY=-1";
    private static final String UTILISATEUR = "sa";
    private static final String MOT_DE_PASSE = "";

    /** Ouvre une nouvelle connexion : l'appelant doit la fermer (try-with-resources). */
    public static Connection ouvrir() throws SQLException {
        return DriverManager.getConnection(URL, UTILISATEUR, MOT_DE_PASSE);
    }

    /** Crée les tables en exécutant le script sql/creation_base.sql (une fois au démarrage). */
    public static void initialiserBase() {
        try (Connection connexion = ouvrir();
             Statement instruction = connexion.createStatement()) {
            instruction.execute("RUNSCRIPT FROM 'sql/creation_base.sql'");
        } catch (SQLException e) {
            throw new BaseDonneesException("Impossible de créer la base", e);
        }
    }
}
