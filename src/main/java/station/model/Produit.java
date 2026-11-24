package station.model;

/** Produit de lavage stocké en millilitres, avec un seuil d'alerte. */
public class Produit {

    private int id;
    private String nom;
    private int stockMl;
    private int seuilMl;

    /** Crée un produit. */
    public Produit(int id, String nom, int stockMl, int seuilMl) {
        this.id = id;
        this.nom = nom;
        this.stockMl = stockMl;
        this.seuilMl = seuilMl;
    }

    /** Vrai si le stock est descendu sous le seuil d'alerte. */
    public boolean estSousSeuil() {
        return stockMl < seuilMl;
    }

    /** Identifiant en base. */
    public int getId() {
        return id;
    }

    /** Modifie l'identifiant (donné par la base après l'ajout). */
    public void setId(int id) {
        this.id = id;
    }

    /** Nom du produit (unique). */
    public String getNom() {
        return nom;
    }

    /** Change le nom. */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /** Quantité en stock, en ml. */
    public int getStockMl() {
        return stockMl;
    }

    /** Change la quantité en stock. */
    public void setStockMl(int stockMl) {
        this.stockMl = stockMl;
    }

    /** Seuil d'alerte, en ml. */
    public int getSeuilMl() {
        return seuilMl;
    }

    /** Change le seuil d'alerte. */
    public void setSeuilMl(int seuilMl) {
        this.seuilMl = seuilMl;
    }

    /** Texte affiché pour le produit. */
    @Override
    public String toString() {
        return id + " - " + nom + " : " + stockMl + " ml (seuil " + seuilMl + " ml)";
    }
}
