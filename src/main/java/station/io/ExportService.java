package station.io;

import station.model.Lavage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Écrit les fichiers exportés : la liste des lavages en CSV et le ticket d'un lavage. */
public class ExportService {

    private static final Path DOSSIER_EXPORTS = Path.of("exports");
    private static final Path DOSSIER_TICKETS = DOSSIER_EXPORTS.resolve("tickets");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Écrit exports/lavages.csv : une ligne par lavage, séparateur ";" (lisible par Excel). */
    public Path exporterLavagesCsv(List<Lavage> lavages) throws IOException {
        List<String> lignes = new ArrayList<>();
        lignes.add("date;immatriculation;formule;laveur;statut;montant");
        lavages.stream().map(this::ligneCsv).forEach(lignes::add);
        Files.createDirectories(DOSSIER_EXPORTS);
        Path fichier = DOSSIER_EXPORTS.resolve("lavages.csv");
        Files.write(fichier, lignes, StandardCharsets.UTF_8);
        Journal.ecrire("EXPORT", lavages.size() + " lavages exportés dans " + fichier);
        return fichier;
    }

    /** Écrit exports/tickets/ticket-<id>.txt avec le détail du lavage et le montant payé. */
    public Path genererTicket(Lavage l) throws IOException {
        List<String> lignes = new ArrayList<>();
        lignes.add("===== STATION DE LAVAGE =====");
        lignes.add("Ticket n° " + l.getId());
        lignes.add("Date      : " + formater(l.getDateCreation()));
        lignes.add("Véhicule  : " + l.getVehicule().getType() + " " + l.getVehicule().getImmatriculation());
        lignes.add("Formule   : " + l.getFormule() + " (" + l.getVehicule().calculerPrix(l.getFormule()) + " MAD)");
        l.getOptions().forEach(o -> lignes.add("Option    : " + o));
        lignes.add("Remise    : " + Math.round(l.getRemise() * 100) + " %");
        lignes.add("Statut    : " + l.getStatut());
        lignes.add("MONTANT PAYÉ : " + l.getMontant() + " MAD");
        Files.createDirectories(DOSSIER_TICKETS);
        Path fichier = DOSSIER_TICKETS.resolve("ticket-" + l.getId() + ".txt");
        Files.write(fichier, lignes, StandardCharsets.UTF_8);
        Journal.ecrire("TICKET", "Ticket du lavage " + l.getId() + " créé");
        return fichier;
    }

    /** Construit la ligne CSV d'un lavage. */
    private String ligneCsv(Lavage l) {
        String laveur = l.getLaveur() == null ? "" : l.getLaveur().getNom();
        return formater(l.getDateCreation()) + ";" + l.getVehicule().getImmatriculation() + ";"
                + l.getFormule() + ";" + laveur + ";" + l.getStatut() + ";" + l.getMontant();
    }

    /** Met une date au format "aaaa-mm-jj hh:mm" (vide si la date est absente). */
    private String formater(LocalDateTime date) {
        return date == null ? "" : date.format(FORMAT);
    }
}
