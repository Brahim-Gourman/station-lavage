package station.dao;

import station.model.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès à la table produit. */
public class ProduitDao implements Dao<Produit> {

    /** Insère le produit et récupère l'identifiant généré. */
    @Override
    public void ajouter(Produit p) {
        String sql = "INSERT INTO produit (nom, stock_ml, seuil_ml) VALUES (?, ?, ?)";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNom());
            ps.setInt(2, p.getStockMl());
            ps.setInt(3, p.getSeuilMl());
            ps.executeUpdate();
            ResultSet cle = ps.getGeneratedKeys();
            cle.next();
            p.setId(cle.getInt(1));
        } catch (SQLException e) {
            throw new BaseDonneesException("Ajout du produit impossible (nom déjà utilisé ?)", e);
        }
    }

    /** Cherche un produit par son identifiant. */
    @Override
    public Optional<Produit> trouverParId(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM produit WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(lire(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture du produit impossible", e);
        }
    }

    /** Retourne tous les produits. */
    @Override
    public List<Produit> lister() {
        List<Produit> produits = new ArrayList<>();
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM produit ORDER BY id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                produits.add(lire(rs));
            }
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture des produits impossible", e);
        }
        return produits;
    }

    /** Met à jour le nom, le stock et le seuil. */
    @Override
    public void modifier(Produit p) {
        String sql = "UPDATE produit SET nom = ?, stock_ml = ?, seuil_ml = ? WHERE id = ?";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setInt(2, p.getStockMl());
            ps.setInt(3, p.getSeuilMl());
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Modification du produit impossible", e);
        }
    }

    /** Supprime le produit. */
    @Override
    public void supprimer(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM produit WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Suppression du produit impossible", e);
        }
    }

    /** Transforme la ligne courante du ResultSet en objet Produit. */
    private Produit lire(ResultSet rs) throws SQLException {
        return new Produit(rs.getInt("id"), rs.getString("nom"),
                rs.getInt("stock_ml"), rs.getInt("seuil_ml"));
    }
}
