package station.dao;

import station.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès à la table client. Chaque méthode ouvre et ferme sa propre connexion. */
public class ClientDao implements Dao<Client> {

    /** Insère le client et récupère l'identifiant généré. */
    @Override
    public void ajouter(Client client) {
        String sql = "INSERT INTO client (nom, telephone) VALUES (?, ?)";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, client.getNom());
            ps.setString(2, client.getTelephone());
            ps.executeUpdate();
            ResultSet cle = ps.getGeneratedKeys();
            cle.next();
            client.setId(cle.getInt(1));
        } catch (SQLException e) {
            throw new BaseDonneesException("Ajout du client impossible (téléphone déjà utilisé ?)", e);
        }
    }

    /** Cherche un client par son identifiant. */
    @Override
    public Optional<Client> trouverParId(int id) {
        String sql = "SELECT * FROM client WHERE id = ?";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(lire(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture du client impossible", e);
        }
    }

    /** Retourne tous les clients. */
    @Override
    public List<Client> lister() {
        List<Client> clients = new ArrayList<>();
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM client ORDER BY id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                clients.add(lire(rs));
            }
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture des clients impossible", e);
        }
        return clients;
    }

    /** Met à jour le nom et le téléphone. */
    @Override
    public void modifier(Client client) {
        String sql = "UPDATE client SET nom = ?, telephone = ? WHERE id = ?";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, client.getNom());
            ps.setString(2, client.getTelephone());
            ps.setInt(3, client.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Modification du client impossible (téléphone déjà utilisé ?)", e);
        }
    }

    /** Supprime le client (ses véhicules sont supprimés par ON DELETE CASCADE). */
    @Override
    public void supprimer(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM client WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Suppression du client impossible", e);
        }
    }

    /** Transforme la ligne courante du ResultSet en objet Client. */
    private Client lire(ResultSet rs) throws SQLException {
        return new Client(rs.getInt("id"), rs.getString("nom"), rs.getString("telephone"));
    }
}
