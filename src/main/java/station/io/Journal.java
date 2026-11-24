package station.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Journal des opérations, écrit dans logs/journal.log. Partagé par tous les threads. */
public class Journal {

    private static final Path FICHIER = Path.of("logs", "journal.log");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Ajoute une ligne "date | type | détail" à la fin du fichier.
     * synchronized : un seul thread écrit à la fois, les lignes ne se mélangent donc pas.
     */
    public static synchronized void ecrire(String type, String detail) {
        String ligne = LocalDateTime.now().format(FORMAT) + " | " + type + " | " + detail
                + System.lineSeparator();
        try {
            Files.createDirectories(FICHIER.getParent());
            Files.writeString(FICHIER, ligne, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture du journal impossible", e);
        }
    }

    /** Relit toutes les lignes du journal (liste vide si le fichier n'existe pas encore). */
    public static synchronized List<String> lire() {
        if (!Files.exists(FICHIER)) {
            return List.of();
        }
        try {
            return Files.readAllLines(FICHIER, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du journal impossible", e);
        }
    }

    /** Chemin du fichier journal. */
    public static Path getFichier() {
        return FICHIER;
    }
}
