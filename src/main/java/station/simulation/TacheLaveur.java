package station.simulation;

import station.dao.LavageDao;
import station.exception.StockInsuffisantException;
import station.io.Journal;
import station.model.Lavage;
import station.model.Laveur;
import station.model.StatutLavage;
import station.service.StockService;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

/** Travail d'un laveur dans la simulation : il prend les lavages de la file un par un. */
public class TacheLaveur implements Runnable {

    /** Une minute simulée dure 10 ms réelles. */
    private static final int MS_PAR_MINUTE = 10;

    private final BlockingQueue<Lavage> file;
    private final Laveur laveur;
    private final StockService stockService;
    private final LavageDao lavageDao;
    private int nbPris = 0;

    /** Reçoit les ressources partagées (file, stock) et le laveur de ce thread. */
    public TacheLaveur(BlockingQueue<Lavage> file, Laveur laveur, StockService stockService,
                       LavageDao lavageDao) {
        this.file = file;
        this.laveur = laveur;
        this.stockService = stockService;
        this.lavageDao = lavageDao;
    }

    /**
     * Boucle du laveur : poll() remet chaque lavage à un seul thread et renvoie null quand
     * la file est vide ; la tâche s'arrête alors.
     */
    @Override
    public void run() {
        Lavage lavage = file.poll();
        while (lavage != null) {
            nbPris++;
            try {
                traiter(lavage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            lavage = file.poll();
        }
    }

    /** Nombre de lavages sortis de la file par ce laveur. */
    public int getNbPris() {
        return nbPris;
    }

    /** Retire le stock, attend la durée simulée, puis enregistre le lavage terminé. */
    private void traiter(Lavage lavage) throws InterruptedException {
        lavage.setLaveur(laveur);
        try {
            stockService.consommer(lavage.getFormule());
        } catch (StockInsuffisantException e) {
            enregistrer(lavage, StatutLavage.ANNULE, e.getMessage());
            return;
        }
        enregistrer(lavage, StatutLavage.EN_COURS, "démarre");
        Thread.sleep((long) lavage.getFormule().getDureeMinutes() * MS_PAR_MINUTE);
        lavage.setDateFin(LocalDateTime.now());
        enregistrer(lavage, StatutLavage.TERMINE, "termine");
    }

    /** Change le statut, l'enregistre en base (une connexion par opération) et l'écrit au journal. */
    private void enregistrer(Lavage lavage, StatutLavage statut, String detail) {
        lavage.setStatut(statut);
        lavageDao.modifier(lavage);
        Journal.ecrire("SIMULATION", laveur.getNom() + " " + detail + " le lavage " + lavage.getId()
                + " (" + lavage.getFormule() + ", " + statut + ")");
    }
}
