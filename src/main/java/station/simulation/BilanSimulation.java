package station.simulation;

import java.util.Map;

/**
 * Résultat d'une simulation et ses trois contrôles de cohérence.
 * Un record est une petite classe immuable : Java crée le constructeur et les getters.
 */
public record BilanSimulation(int crees, int prisDansLaFile, long termines, long annules,
                              Map<String, Integer> stockInitial, Map<String, Integer> consommation,
                              Map<String, Integer> stockFinal) {

    /** Contrôle 1 : terminés + annulés = créés, et chaque lavage est sorti une seule fois de la file. */
    public boolean aucunLavagePerdu() {
        return termines + annules == crees && prisDansLaFile == crees;
    }

    /** Contrôle 2 : stock final = stock initial − consommation des lavages terminés. */
    public boolean stockJuste() {
        return stockInitial.keySet().stream()
                .allMatch(nom -> stockFinal.get(nom)
                        == stockInitial.get(nom) - consommation.getOrDefault(nom, 0));
    }

    /** Contrôle 3 : aucun stock négatif. */
    public boolean aucunStockNegatif() {
        return stockFinal.values().stream().allMatch(quantite -> quantite >= 0);
    }

    /** Vrai si les trois contrôles sont bons. */
    public boolean estCoherent() {
        return aucunLavagePerdu() && stockJuste() && aucunStockNegatif();
    }

    /** Texte du bilan, affiché par la console et par JavaFX. */
    public String texte() {
        return "Lavages créés : " + crees + " | pris dans la file : " + prisDansLaFile
                + " | terminés : " + termines + " | annulés : " + annules + "\n"
                + "Stock initial : " + stockInitial + "\n"
                + "Consommation  : " + consommation + "\n"
                + "Stock final   : " + stockFinal + "\n"
                + "Contrôle terminés + annulés = créés : " + ok(aucunLavagePerdu()) + "\n"
                + "Contrôle stock final = initial - consommation : " + ok(stockJuste()) + "\n"
                + "Contrôle aucun stock négatif : " + ok(aucunStockNegatif());
    }

    /** Transforme un booléen en "OK" ou "ERREUR". */
    private String ok(boolean controle) {
        return controle ? "OK" : "ERREUR";
    }
}
