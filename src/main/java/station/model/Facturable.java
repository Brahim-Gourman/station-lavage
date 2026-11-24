package station.model;

/** Tout ce qui a un montant à payer : un lavage ou une option. */
public interface Facturable {

    /** Retourne le montant à payer, en dirhams. */
    double calculerMontant();
}
