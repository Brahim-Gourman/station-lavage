package station.service;

import station.dao.ClientDao;
import station.dao.Connexion;
import station.dao.LavageDao;
import station.dao.LaveurDao;
import station.dao.ProduitDao;
import station.dao.VehiculeDao;
import station.io.ExportService;
import station.simulation.SimulationService;

/**
 * Crée une seule fois tous les DAO et services, puis les partage.
 * La console et JavaFX utilisent cette même classe : aucune logique n'est copiée.
 */
public class Services {

    public final ClientService clients;
    public final VehiculeService vehicules;
    public final LaveurService laveurs;
    public final StockService stock;
    public final LavageService lavages;
    public final RechercheService recherche;
    public final StatistiqueService statistiques;
    public final ExportService export;
    public final SimulationService simulation;

    /** Prépare la base puis assemble les objets. */
    public Services() {
        Connexion.initialiserBase();
        ClientDao clientDao = new ClientDao();
        VehiculeDao vehiculeDao = new VehiculeDao();
        LaveurDao laveurDao = new LaveurDao();
        ProduitDao produitDao = new ProduitDao();
        LavageDao lavageDao = new LavageDao(vehiculeDao, laveurDao);
        clients = new ClientService(clientDao, vehiculeDao, lavageDao);
        vehicules = new VehiculeService(vehiculeDao, clientDao, lavageDao);
        laveurs = new LaveurService(laveurDao, lavageDao);
        stock = new StockService(produitDao);
        lavages = new LavageService(lavageDao, vehiculeDao, laveurDao, stock);
        recherche = new RechercheService(clientDao, vehiculeDao, lavageDao);
        statistiques = new StatistiqueService(lavageDao, produitDao);
        export = new ExportService();
        simulation = new SimulationService(lavages, laveurs, clients, vehicules, recherche, stock, lavageDao);
    }
}
