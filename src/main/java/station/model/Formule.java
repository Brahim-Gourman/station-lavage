package station.model;

import java.util.Map;

/** Les trois formules de lavage, avec leur prix, leur durée et les produits consommés. */
public enum Formule {

    EXPRESS(30, 15, Map.of("shampooing", 200)),
    COMPLET(50, 30, Map.of("shampooing", 300)),
    PREMIUM(80, 45, Map.of("shampooing", 300, "cire", 100));

    private final double prixBase;
    private final int dureeMinutes;
    private final Map<String, Integer> consommation;

    Formule(double prixBase, int dureeMinutes, Map<String, Integer> consommation) {
        this.prixBase = prixBase;
        this.dureeMinutes = dureeMinutes;
        this.consommation = consommation;
    }

    /** Prix de base de la formule, en dirhams. */
    public double getPrixBase() {
        return prixBase;
    }

    /** Durée simulée du lavage, en minutes. */
    public int getDureeMinutes() {
        return dureeMinutes;
    }

    /** Produits consommés : clé = nom du produit, valeur = quantité en ml. */
    public Map<String, Integer> getConsommation() {
        return consommation;
    }
}
