package station;

import station.console.MenuConsole;
import station.service.Services;

/** Point d'entrée de la version console. */
public class Main {

    /** Prépare la base et les services, puis lance le menu. */
    public static void main(String[] args) {
        Services services = new Services();
        new MenuConsole(services).lancer();
    }
}
