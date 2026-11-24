package station.service;

import station.dao.LavageDao;
import station.dao.ProduitDao;
import station.model.Formule;
import station.model.Lavage;
import station.model.Produit;
import station.model.StatutLavage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Statistiques (F8), toutes écrites avec l'API Stream, sans boucle.
 * Le chiffre d'affaires ne compte que les lavages TERMINE (l'argent réellement encaissé).
 */
public class StatistiqueService {

    private final LavageDao lavageDao;
    private final ProduitDao produitDao;

    /** Reçoit les DAO dont il a besoin. */
    public StatistiqueService(LavageDao lavageDao, ProduitDao produitDao) {
        this.lavageDao = lavageDao;
        this.produitDao = produitDao;
    }

    /** Chiffre d'affaires total. */
    public double caTotal() {
        return lavagesTermines().stream().mapToDouble(Lavage::getMontant).sum();
    }

    /** Chiffre d'affaires par type de véhicule (MOTO, VOITURE, CAMION). */
    public Map<String, Double> caParTypeVehicule() {
        return lavagesTermines().stream()
                .collect(Collectors.groupingBy(l -> l.getVehicule().getType(),
                        Collectors.summingDouble(Lavage::getMontant)));
    }

    /** Chiffre d'affaires par formule. */
    public Map<Formule, Double> caParFormule() {
        return lavagesTermines().stream()
                .collect(Collectors.groupingBy(Lavage::getFormule,
                        Collectors.summingDouble(Lavage::getMontant)));
    }

    /** Nombre de lavages terminés par chaque laveur (clé = nom du laveur). */
    public Map<String, Long> lavagesParLaveur() {
        return lavagesTermines().stream()
                .filter(l -> l.getLaveur() != null)
                .collect(Collectors.groupingBy(l -> l.getLaveur().getNom(), Collectors.counting()));
    }

    /** Laveur qui a terminé le plus de lavages (vide s'il n'y a aucun lavage). */
    public Optional<String> meilleurLaveur() {
        return lavagesParLaveur().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /** Formule la plus demandée, en comptant tous les lavages créés. */
    public Optional<Formule> formuleLaPlusDemandee() {
        return lavageDao.lister().stream()
                .collect(Collectors.groupingBy(Lavage::getFormule, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /** Panier moyen : montant moyen d'un lavage terminé (0 s'il n'y en a pas). */
    public double panierMoyen() {
        return lavagesTermines().stream().mapToDouble(Lavage::getMontant).average().orElse(0);
    }

    /** Produits dont le stock est sous le seuil d'alerte. */
    public List<Produit> produitsSousSeuil() {
        return produitDao.lister().stream().filter(Produit::estSousSeuil).toList();
    }

    /** Lavages terminés, base de tous les calculs de chiffre d'affaires. */
    private List<Lavage> lavagesTermines() {
        return lavageDao.lister().stream()
                .filter(l -> l.getStatut() == StatutLavage.TERMINE)
                .toList();
    }
}
