package station.dao;

import station.model.Camion;
import station.model.Moto;
import station.model.Vehicule;
import station.model.Voiture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès à la table vehicule. La colonne "type" permet de recréer Moto, Voiture ou Camion. */
public class VehiculeDao implements Dao<Vehicule> {

    /** Insère le véhicule et récupère l'identifiant généré. */
    @Override
    public void ajouter(Vehicule v) {
        String sql = "INSERT INTO vehicule (client_id, immatriculation, type, suv, nb_essieux) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, v);
            ps.executeUpdate();
            ResultSet cle = ps.getGeneratedKeys();
            cle.next();
            v.setId(cle.getInt(1));
        } catch (SQLException e) {
            throw new BaseDonneesException("Ajout du véhicule impossible (immatriculation déjà utilisée ?)", e);
        }
    }

    /** Cherche un véhicule par son identifiant. */
    @Override
    public Optional<Vehicule> trouverParId(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM vehicule WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(lire(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture du véhicule impossible", e);
        }
    }

    /** Retourne tous les véhicules. */
    @Override
    public List<Vehicule> lister() {
        List<Vehicule> vehicules = new ArrayList<>();
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM vehicule ORDER BY id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                vehicules.add(lire(rs));
            }
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture des véhicules impossible", e);
        }
        return vehicules;
    }

    /** Met à jour toutes les colonnes du véhicule. */
    @Override
    public void modifier(Vehicule v) {
        String sql = "UPDATE vehicule SET client_id = ?, immatriculation = ?, type = ?, suv = ?, nb_essieux = ? WHERE id = ?";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, v);
            ps.setInt(6, v.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Modification du véhicule impossible", e);
        }
    }

    /** Supprime le véhicule. */
    @Override
    public void supprimer(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM vehicule WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Suppression du véhicule impossible", e);
        }
    }

    /** Remplit les cinq premiers paramètres communs à INSERT et UPDATE. */
    private void remplir(PreparedStatement ps, Vehicule v) throws SQLException {
        ps.setInt(1, v.getClientId());
        ps.setString(2, v.getImmatriculation());
        ps.setString(3, v.getType());
        ps.setBoolean(4, v instanceof Voiture voiture && voiture.isSuv());
        ps.setInt(5, v instanceof Camion camion ? camion.getNbEssieux() : 0);
    }

    /** Recrée le bon objet (Moto, Voiture ou Camion) selon la colonne type. */
    private Vehicule lire(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int clientId = rs.getInt("client_id");
        String immat = rs.getString("immatriculation");
        return switch (rs.getString("type")) {
            case "MOTO" -> new Moto(id, clientId, immat);
            case "VOITURE" -> new Voiture(id, clientId, immat, rs.getBoolean("suv"));
            default -> new Camion(id, clientId, immat, rs.getInt("nb_essieux"));
        };
    }
}
