-- Création des six tables de la station de lavage (base H2).
-- Le script peut être relancé sans risque : il ne recrée rien qui existe déjà.

CREATE TABLE IF NOT EXISTS client (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nom       VARCHAR(100) NOT NULL,
    telephone VARCHAR(20)  NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS vehicule (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    client_id       INT          NOT NULL,
    immatriculation VARCHAR(20)  NOT NULL UNIQUE,
    type            VARCHAR(10)  NOT NULL,
    suv             BOOLEAN      NOT NULL DEFAULT FALSE,
    nb_essieux      INT          NOT NULL DEFAULT 0,
    FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS laveur (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    nom   VARCHAR(100) NOT NULL,
    actif BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS produit (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    nom      VARCHAR(50) NOT NULL UNIQUE,
    stock_ml INT         NOT NULL,
    seuil_ml INT         NOT NULL
);

CREATE TABLE IF NOT EXISTS lavage (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    vehicule_id   INT          NOT NULL,
    laveur_id     INT,
    formule       VARCHAR(10)  NOT NULL,
    statut        VARCHAR(12)  NOT NULL,
    date_creation TIMESTAMP    NOT NULL,
    date_fin      TIMESTAMP,
    remise        DOUBLE       NOT NULL,
    montant       DOUBLE       NOT NULL,
    FOREIGN KEY (vehicule_id) REFERENCES vehicule(id),
    FOREIGN KEY (laveur_id)   REFERENCES laveur(id)
);

CREATE TABLE IF NOT EXISTS lavage_option (
    lavage_id INT          NOT NULL,
    nom       VARCHAR(50)  NOT NULL,
    prix      DOUBLE       NOT NULL,
    PRIMARY KEY (lavage_id, nom),
    FOREIGN KEY (lavage_id) REFERENCES lavage(id) ON DELETE CASCADE
);

-- Les deux produits utilisés par les formules (ajoutés une seule fois).
INSERT INTO produit (nom, stock_ml, seuil_ml)
    SELECT 'shampooing', 20000, 3000
    WHERE NOT EXISTS (SELECT 1 FROM produit WHERE nom = 'shampooing');

INSERT INTO produit (nom, stock_ml, seuil_ml)
    SELECT 'cire', 5000, 1000
    WHERE NOT EXISTS (SELECT 1 FROM produit WHERE nom = 'cire');
