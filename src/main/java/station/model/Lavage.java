package station.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Un lavage relie un véhicule, une formule, des options et (plus tard) un laveur. */
public class Lavage implements Facturable {

    /** Option ajoutée à un lavage. Elle n'existe qu'à l'intérieur d'un lavage : classe imbriquée statique. */
    public static class Option implements Facturable {

        public static final Option DESODORISANT = new Option("desodorisant", 10);
        public static final Option CIRAGE_PNEUS = new Option("cirage des pneus", 15);
        public static final Option NETTOYAGE_MOTEUR = new Option("nettoyage moteur", 40);

        private final String nom;
        private final double prix;

        /** Crée une option (utilisé aussi par le DAO pour la relire en base). */
        public Option(String nom, double prix) {
            this.nom = nom;
            this.prix = prix;
        }

        /** Les trois options proposées par la station. */
        public static List<Option> catalogue() {
            return List.of(DESODORISANT, CIRAGE_PNEUS, NETTOYAGE_MOTEUR);
        }

        /** Le montant d'une option est son prix. */
        @Override
        public double calculerMontant() {
            return prix;
        }

        /** Nom de l'option. */
        public String getNom() {
            return nom;
        }

        /** Prix de l'option, en dirhams. */
        public double getPrix() {
            return prix;
        }

        /** Deux options sont égales si elles ont le même nom : le Set refuse donc les doublons. */
        @Override
        public boolean equals(Object autre) {
            return autre instanceof Option o && nom.equals(o.nom);
        }

        /** Code de hachage cohérent avec equals. */
        @Override
        public int hashCode() {
            return Objects.hash(nom);
        }

        /** Texte affiché : nom et prix. */
        @Override
        public String toString() {
            return nom + " (" + prix + " MAD)";
        }
    }

    private int id;
    private Vehicule vehicule;
    private Laveur laveur;
    private Formule formule;
    private StatutLavage statut = StatutLavage.EN_ATTENTE;
    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateFin;
    private double remise;
    private double montant;
    private final Set<Option> options = new HashSet<>();

    /** Crée un lavage en attente, sans laveur. */
    public Lavage(Vehicule vehicule, Formule formule, double remise) {
        this.vehicule = vehicule;
        this.formule = formule;
        this.remise = remise;
    }

    /** Ajoute une option ; une même option n'est jamais comptée deux fois (Set). */
    public void ajouterOption(Option option) {
        options.add(option);
    }

    /** Montant = (prix du véhicule + somme des options) × (1 − remise), arrondi au centime. */
    @Override
    public double calculerMontant() {
        double totalOptions = options.stream().mapToDouble(Option::calculerMontant).sum();
        double brut = vehicule.calculerPrix(formule) + totalOptions;
        return Math.round(brut * (1 - remise) * 100) / 100.0;
    }

    /** Identifiant en base. */
    public int getId() {
        return id;
    }

    /** Modifie l'identifiant (donné par la base après l'ajout). */
    public void setId(int id) {
        this.id = id;
    }

    /** Véhicule lavé. */
    public Vehicule getVehicule() {
        return vehicule;
    }

    /** Laveur attribué, ou null s'il n'y en a pas encore. */
    public Laveur getLaveur() {
        return laveur;
    }

    /** Attribue un laveur. */
    public void setLaveur(Laveur laveur) {
        this.laveur = laveur;
    }

    /** Formule choisie. */
    public Formule getFormule() {
        return formule;
    }

    /** Statut actuel. */
    public StatutLavage getStatut() {
        return statut;
    }

    /** Change le statut. */
    public void setStatut(StatutLavage statut) {
        this.statut = statut;
    }

    /** Date de création. */
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    /** Change la date de création (utilisé par le DAO). */
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    /** Date de fin, ou null si le lavage n'est pas terminé. */
    public LocalDateTime getDateFin() {
        return dateFin;
    }

    /** Change la date de fin. */
    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    /** Remise appliquée (0.1 pour 10 %). */
    public double getRemise() {
        return remise;
    }

    /** Montant enregistré pour ce lavage. */
    public double getMontant() {
        return montant;
    }

    /** Enregistre le montant. */
    public void setMontant(double montant) {
        this.montant = montant;
    }

    /** Options choisies. */
    public Set<Option> getOptions() {
        return options;
    }

    /** Texte affiché pour le lavage. */
    @Override
    public String toString() {
        String nomLaveur = laveur == null ? "aucun laveur" : laveur.getNom();
        return "Lavage " + id + " | " + vehicule.getImmatriculation() + " | " + formule
                + " | " + statut + " | " + nomLaveur + " | " + montant + " MAD";
    }
}
