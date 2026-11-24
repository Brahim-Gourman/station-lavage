package station.simulation;

import station.dao.LavageDao;
import station.exception.EntiteIntrouvableException;
import station.exception.FormuleNonDisponibleException;
import station.io.Journal;
import station.model.Client;
import station.model.Formule;
import station.model.Lavage;
import station.model.Laveur;
import station.model.StatutLavage;
import station.model.Vehicule;
import station.model.Voiture;
import station.service.ClientService;
import station.service.LavageService;
import station.service.LaveurService;
import station.service.RechercheService;
import station.service.StockService;
import station.service.VehiculeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Simulation (F9) : N laveurs (threads) partagent une file de lavages et le stock. */
public class SimulationService {

    private static final String IMMATRICULATION = "SIM-0001";

    private final LavageService lavageService;
    private final LaveurService laveurService;
    private final ClientService clientService;
    private final VehiculeService vehiculeService;
    private final RechercheService rechercheService;
    private final StockService stockService;
    private final LavageDao lavageDao;

    /** Reçoit les services existants : la simulation réutilise les mêmes règles métier. */
    public SimulationService(LavageService lavageService, LaveurService laveurService,
                             ClientService clientService, VehiculeService vehiculeService,
                             RechercheService rechercheService, StockService stockService,
                             LavageDao lavageDao) {
        this.lavageService = lavageService;
        this.laveurService = laveurService;
        this.clientService = clientService;
        this.vehiculeService = vehiculeService;
        this.rechercheService = rechercheService;
        this.stockService = stockService;
        this.lavageDao = lavageDao;
    }

    /** Crée les lavages, remplit la file, lance les laveurs, attend la fin et retourne le bilan. */
    public BilanSimulation lancer(int nbLaveurs, int nbLavages) throws InterruptedException,
            EntiteIntrouvableException, FormuleNonDisponibleException {
        if (nbLaveurs < 1 || nbLavages < 1) {
            throw new IllegalArgumentException("Il faut au moins 1 laveur et 1 lavage.");
        }
        Journal.ecrire("SIMULATION", "Début : " + nbLaveurs + " laveurs, " + nbLavages + " lavages");
        List<Laveur> laveurs = preparerLaveurs(nbLaveurs);
        Vehicule vehicule = preparerVehicule();
        Map<String, Integer> stockInitial = stockService.stockParNom();
        BlockingQueue<Lavage> file = remplirFile(vehicule, nbLavages);
        List<Lavage> crees = new ArrayList<>(file);
        List<TacheLaveur> taches = executer(file, laveurs);
        BilanSimulation bilan = faireBilan(crees, taches, stockInitial);
        Journal.ecrire("SIMULATION", "Fin : cohérent = " + bilan.estCoherent());
        return bilan;
    }

    /** Réutilise ou crée les laveurs "Laveur sim 1" à "Laveur sim N". */
    private List<Laveur> preparerLaveurs(int nbLaveurs) {
        return IntStream.rangeClosed(1, nbLaveurs)
                .mapToObj(i -> laveurSimulation("Laveur sim " + i))
                .toList();
    }

    /** Cherche un laveur actif par son nom ; le crée s'il n'existe pas. */
    private Laveur laveurSimulation(String nom) {
        return laveurService.lister().stream()
                .filter(l -> l.isActif() && l.getNom().equals(nom))
                .findFirst()
                .orElseGet(() -> laveurService.ajouter(nom));
    }

    /** Réutilise ou crée la voiture de simulation et son client. */
    private Vehicule preparerVehicule() throws EntiteIntrouvableException {
        Optional<Vehicule> existant = rechercheService.vehiculeParImmatriculation(IMMATRICULATION);
        if (existant.isPresent()) {
            return existant.get();
        }
        Client client = clientService.ajouter("Client simulation", "0000000000");
        Vehicule vehicule = new Voiture(0, client.getId(), IMMATRICULATION, false);
        vehiculeService.ajouter(vehicule);
        return vehicule;
    }

    /** Crée les lavages (Express, Complet, Premium à tour de rôle) et les met dans la file. */
    private BlockingQueue<Lavage> remplirFile(Vehicule vehicule, int nbLavages)
            throws EntiteIntrouvableException, FormuleNonDisponibleException {
        BlockingQueue<Lavage> file = new LinkedBlockingQueue<>();
        Formule[] formules = Formule.values();
        for (int i = 0; i < nbLavages; i++) {
            file.add(lavageService.creer(vehicule.getId(), formules[i % formules.length], Set.of()));
        }
        return file;
    }

    /** Lance une TacheLaveur par laveur dans un ExecutorService et attend qu'elles finissent. */
    private List<TacheLaveur> executer(BlockingQueue<Lavage> file, List<Laveur> laveurs)
            throws InterruptedException {
        ExecutorService executeur = Executors.newFixedThreadPool(laveurs.size());
        List<TacheLaveur> taches = laveurs.stream()
                .map(l -> new TacheLaveur(file, l, stockService, lavageDao))
                .toList();
        taches.forEach(executeur::execute);
        executeur.shutdown();
        executeur.awaitTermination(10, TimeUnit.MINUTES);
        return taches;
    }

    /** Relit les lavages en base et calcule les chiffres du bilan. */
    private BilanSimulation faireBilan(List<Lavage> crees, List<TacheLaveur> taches,
                                       Map<String, Integer> stockInitial) {
        List<Lavage> relus = crees.stream()
                .map(l -> lavageDao.trouverParId(l.getId()).orElseThrow())
                .toList();
        long termines = relus.stream().filter(l -> l.getStatut() == StatutLavage.TERMINE).count();
        long annules = relus.stream().filter(l -> l.getStatut() == StatutLavage.ANNULE).count();
        int pris = taches.stream().mapToInt(TacheLaveur::getNbPris).sum();
        return new BilanSimulation(crees.size(), pris, termines, annules, stockInitial,
                consommation(relus), stockService.stockParNom());
    }

    /** Quantité de chaque produit utilisée par les lavages terminés (nom → ml). */
    private Map<String, Integer> consommation(List<Lavage> lavages) {
        return lavages.stream()
                .filter(l -> l.getStatut() == StatutLavage.TERMINE)
                .flatMap(l -> l.getFormule().getConsommation().entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingInt(Map.Entry::getValue)));
    }
}
