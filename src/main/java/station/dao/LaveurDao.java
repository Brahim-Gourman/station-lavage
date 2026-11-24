package station.dao;

import station.model.Laveur;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès à la table laveur. */
public class LaveurDao implements Dao<Laveur> {

    /** Insère le laveur et récupère l'identifiant généré. */
    @Override
    public void ajouter(Laveur laveur) {
        String sql = "INSERT INTO laveur (nom, actif) VALUES (?, ?)";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, laveur.getNom());
            ps.setBoolean(2, laveur.isActif());
            ps.executeUpdate();
            ResultSet cle = ps.getGeneratedKeys();
            cle.next();
            laveur.setId(cle.getInt(1));
        } catch (SQLException e) {
            throw new BaseDonneesException("Ajout du laveur impossible", e);
        }
    }

    /** Cherche un laveur par son identifiant. */
    @Override
    public Optional<Laveur> trouverParId(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM laveur WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(lire(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture du laveur impossible", e);
        }
    }

    /** Retourne tous les laveurs. */
    @Override
    public List<Laveur> lister() {
        List<Laveur> laveurs = new ArrayList<>();
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM laveur ORDER BY id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                laveurs.add(lire(rs));
            }
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture des laveurs impossible", e);
        }
        return laveurs;
    }

    /** Met à jour le nom et l'état actif. */
    @Override
    public void modifier(Laveur laveur) {
        String sql = "UPDATE laveur SET nom = ?, actif = ? WHERE id = ?";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, laveur.getNom());
            ps.setBoolean(2, laveur.isActif());
            ps.setInt(3, laveur.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Modification du laveur impossible", e);
        }
    }

    /** Supprime le laveur. */
    @Override
    public void supprimer(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM laveur WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Suppression du laveur impossible", e);
        }
    }

    /** Transforme la ligne courante du ResultSet en objet Laveur. */
    private Laveur lire(ResultSet rs) throws SQLException {
        return new Laveur(rs.getInt("id"), rs.getString("nom"), rs.getBoolean("actif"));
    }
}
