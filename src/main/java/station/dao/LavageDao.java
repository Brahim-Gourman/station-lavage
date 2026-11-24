package station.dao;

import station.model.Formule;
import station.model.Laveur;
import station.model.Lavage;
import station.model.StatutLavage;
import station.model.Vehicule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux tables lavage et lavage_option. Thread-safe : une connexion par opération. */
public class LavageDao implements Dao<Lavage> {

    private final VehiculeDao vehiculeDao;
    private final LaveurDao laveurDao;

    /** Le DAO des lavages s'appuie sur ceux des véhicules et des laveurs pour recréer les objets. */
    public LavageDao(VehiculeDao vehiculeDao, LaveurDao laveurDao) {
        this.vehiculeDao = vehiculeDao;
        this.laveurDao = laveurDao;
    }

    /** Insère le lavage puis ses options, avec la même connexion. */
    @Override
    public void ajouter(Lavage l) {
        String sql = "INSERT INTO lavage (vehicule_id, laveur_id, formule, statut, date_creation, "
                + "date_fin, remise, montant) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, l.getVehicule().getId());
            remplirSuivi(ps, l, 2);
            ps.executeUpdate();
            ResultSet cle = ps.getGeneratedKeys();
            cle.next();
            l.setId(cle.getInt(1));
            ajouterOptions(c, l);
        } catch (SQLException e) {
            throw new BaseDonneesException("Ajout du lavage impossible", e);
        }
    }

    /** Cherche un lavage par son identifiant. */
    @Override
    public Optional<Lavage> trouverParId(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM lavage WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(lire(c, rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture du lavage impossible", e);
        }
    }

    /** Retourne tous les lavages, du plus ancien au plus récent. */
    @Override
    public List<Lavage> lister() {
        List<Lavage> lavages = new ArrayList<>();
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM lavage ORDER BY id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lavages.add(lire(c, rs));
            }
        } catch (SQLException e) {
            throw new BaseDonneesException("Lecture des lavages impossible", e);
        }
        return lavages;
    }

    /** Enregistre le laveur, le statut, les dates, la remise et le montant. */
    @Override
    public void modifier(Lavage l) {
        String sql = "UPDATE lavage SET laveur_id = ?, formule = ?, statut = ?, date_creation = ?, "
                + "date_fin = ?, remise = ?, montant = ? WHERE id = ?";
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            remplirSuivi(ps, l, 1);
            ps.setInt(8, l.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Modification du lavage impossible", e);
        }
    }

    /** Supprime le lavage (ses options partent avec ON DELETE CASCADE). */
    @Override
    public void supprimer(int id) {
        try (Connection c = Connexion.ouvrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM lavage WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BaseDonneesException("Suppression du lavage impossible", e);
        }
    }

    /** Remplit les 7 paramètres de suivi à partir de la position "debut". */
    private void remplirSuivi(PreparedStatement ps, Lavage l, int debut) throws SQLException {
        ps.setObject(debut, l.getLaveur() == null ? null : l.getLaveur().getId());
        ps.setString(debut + 1, l.getFormule().name());
        ps.setString(debut + 2, l.getStatut().name());
        ps.setObject(debut + 3, l.getDateCreation());
        ps.setObject(debut + 4, l.getDateFin());
        ps.setDouble(debut + 5, l.getRemise());
        ps.setDouble(debut + 6, l.getMontant());
    }

    /** Insère une ligne lavage_option par option choisie. */
    private void ajouterOptions(Connection c, Lavage l) throws SQLException {
        String sql = "INSERT INTO lavage_option (lavage_id, nom, prix) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Lavage.Option option : l.getOptions()) {
                ps.setInt(1, l.getId());
                ps.setString(2, option.getNom());
                ps.setDouble(3, option.getPrix());
                ps.executeUpdate();
            }
        }
    }

    /** Recrée un Lavage complet : véhicule, laveur, statut, dates et options. */
    private Lavage lire(Connection c, ResultSet rs) throws SQLException {
        Vehicule vehicule = vehiculeDao.trouverParId(rs.getInt("vehicule_id")).orElseThrow();
        Formule formule = Formule.valueOf(rs.getString("formule"));
        Lavage l = new Lavage(vehicule, formule, rs.getDouble("remise"));
        l.setId(rs.getInt("id"));
        l.setStatut(StatutLavage.valueOf(rs.getString("statut")));
        l.setDateCreation(rs.getObject("date_creation", LocalDateTime.class));
        l.setDateFin(rs.getObject("date_fin", LocalDateTime.class));
        l.setMontant(rs.getDouble("montant"));
        l.setLaveur(lireLaveur(rs));
        lireOptions(c, l);
        return l;
    }

    /** Retourne le laveur du lavage, ou null si la colonne laveur_id est vide. */
    private Laveur lireLaveur(ResultSet rs) throws SQLException {
        int laveurId = rs.getInt("laveur_id");
        if (rs.wasNull()) {
            return null;
        }
        return laveurDao.trouverParId(laveurId).orElse(null);
    }

    /** Ajoute au lavage les options lues dans la table lavage_option. */
    private void lireOptions(Connection c, Lavage l) throws SQLException {
        String sql = "SELECT nom, prix FROM lavage_option WHERE lavage_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, l.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                l.ajouterOption(new Lavage.Option(rs.getString("nom"), rs.getDouble("prix")));
            }
        }
    }
}
