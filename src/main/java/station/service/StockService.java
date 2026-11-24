package station.service;

import station.dao.ProduitDao;
import station.exception.EntiteIntrouvableException;
import station.exception.StockInsuffisantException;
import station.io.Journal;
import station.model.Formule;
import station.model.Produit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestion des produits et du stock (F4). Une seule instance est partagée par toute l'application,
 * pour que les méthodes synchronized protègent vraiment le stock.
 */
public class StockService {

    private final ProduitDao produitDao;

    /** Reçoit le DAO des produits. */
    public StockService(ProduitDao produitDao) {
        this.produitDao = produitDao;
    }

    /**
     * Retire les produits d'une formule. synchronized : la vérification, le retrait et la mise
     * à jour en base forment un seul bloc ; deux threads ne peuvent donc pas vider le stock en même temps.
     * Si un produit manque, rien n'est retiré.
     */
    public synchronized void consommer(Formule formule) throws StockInsuffisantException {
        Map<String, Produit> produits = produitsParNom();
        Optional<String> manquant = formule.getConsommation().entrySet().stream()
                .filter(e -> stockDe(produits, e.getKey()) < e.getValue())
                .map(Map.Entry::getKey)
                .findFirst();
        if (manquant.isPresent()) {
            throw new StockInsuffisantException("Stock insuffisant en " + manquant.get());
        }
        formule.getConsommation().forEach((nom, quantite) -> retirer(produits.get(nom), quantite));
    }

    /** Ajoute une quantité au stock d'un produit. */
    public synchronized void reapprovisionner(int id, int quantiteMl) throws EntiteIntrouvableException {
        if (quantiteMl <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive.");
        }
        Produit produit = trouver(id);
        produit.setStockMl(produit.getStockMl() + quantiteMl);
        produitDao.modifier(produit);
        Journal.ecrire("STOCK", "Réapprovisionnement de " + quantiteMl + " ml : " + produit);
    }

    /** Crée un nouveau produit. */
    public Produit ajouter(String nom, int stockMl, int seuilMl) {
        Verification.texteNonVide(nom, "nom");
        Verification.positif(stockMl, "stock");
        Verification.positif(seuilMl, "seuil");
        Produit produit = new Produit(0, nom, stockMl, seuilMl);
        produitDao.ajouter(produit);
        Journal.ecrire("PRODUIT", "Ajout de " + produit);
        return produit;
    }

    /** Change le nom et le seuil d'un produit (le stock change seulement par réapprovisionnement). */
    public void modifier(int id, String nom, int seuilMl) throws EntiteIntrouvableException {
        Verification.texteNonVide(nom, "nom");
        Verification.positif(seuilMl, "seuil");
        Produit produit = trouver(id);
        produit.setNom(nom);
        produit.setSeuilMl(seuilMl);
        produitDao.modifier(produit);
        Journal.ecrire("PRODUIT", "Modification de " + produit);
    }

    /** Supprime un produit. */
    public void supprimer(int id) throws EntiteIntrouvableException {
        Produit produit = trouver(id);
        produitDao.supprimer(id);
        Journal.ecrire("PRODUIT", "Suppression de " + produit);
    }

    /** Retourne le produit qui a cet identifiant. */
    public Produit trouver(int id) throws EntiteIntrouvableException {
        return produitDao.trouverParId(id)
                .orElseThrow(() -> new EntiteIntrouvableException("Produit", id));
    }

    /** Retourne tous les produits. */
    public List<Produit> lister() {
        return produitDao.lister();
    }

    /** Stock actuel de chaque produit : nom → quantité en ml. */
    public Map<String, Integer> stockParNom() {
        return produitDao.lister().stream()
                .collect(Collectors.toMap(Produit::getNom, Produit::getStockMl));
    }

    /** Lit les produits en base, rangés par nom. */
    private Map<String, Produit> produitsParNom() {
        return produitDao.lister().stream()
                .collect(Collectors.toMap(Produit::getNom, Function.identity()));
    }

    /** Stock d'un produit, ou 0 s'il n'existe pas. */
    private int stockDe(Map<String, Produit> produits, String nom) {
        Produit produit = produits.get(nom);
        return produit == null ? 0 : produit.getStockMl();
    }

    /** Retire une quantité d'un produit et enregistre le nouveau stock. */
    private void retirer(Produit produit, int quantiteMl) {
        produit.setStockMl(produit.getStockMl() - quantiteMl);
        produitDao.modifier(produit);
    }
}
