# Gestion intelligente des stages — Projet SOA

Plateforme orientée services : **3 services SOAP** (JAX-WS), **3 services REST** (JAX-RS), un **processus BPMN 2.0**
exécuté par un moteur embarqué (Flowable) qui orchestre ces services, et une **interface web** par acteur (bonus).

## Démarrage rapide

Prérequis : Java 21, Maven 3.9 et, pour voir les e-mails, [Mailpit](https://mailpit.axllent.org/)
(un seul exécutable : `mailpit`, ou `docker run -p 8025:8025 -p 1025:1025 axllent/mailpit`).

```bash
mailpit                # boîte e-mail de test : http://localhost:8025 (SMTP sur le port 1025)
mvn package            # compile, exécute les 28 tests d'intégration, produit target/gestion-stages.jar
java -jar target/gestion-stages.jar
```

Sans Mailpit, la plateforme fonctionne quand même : les notifications restent consultables dans l'interface
(statut `ECHEC_ENVOI`). Pour désactiver complètement l'envoi : `-Dmail.actif=false`.

| Ressource | URL |
|---|---|
| Interface web | http://localhost:8090/ |
| WSDL Étudiants / Entreprises / Offres | http://localhost:8090/ws/etudiants?wsdl · `/ws/entreprises?wsdl` · `/ws/offres?wsdl` |
| API REST | http://localhost:8090/api/candidatures · `/api/notifications` · `/api/stages` · `/api/processus` |
| Boîte e-mail (Mailpit) | http://localhost:8025 |

Le port par défaut est **8090** (8080 étant souvent pris par Jenkins/Tomcat). Pour en changer : `java -Dserver.port=9000 -jar target/gestion-stages.jar`.

Par défaut, les données sont dans une base H2 locale (`./data`). Supprimer ce dossier réinitialise le jeu de démonstration.

### Avec PostgreSQL

```bash
docker compose up --build      # plateforme + PostgreSQL + Mailpit
```

ou, avec un PostgreSQL existant :

```bash
java -Ddb.url=jdbc:postgresql://localhost:5432/stages -Ddb.user=stages -Ddb.password=stages \
     -Dbpmn.db.url=jdbc:postgresql://localhost:5432/flowable -Dbpmn.db.user=stages -Dbpmn.db.password=stages \
     -jar target/gestion-stages.jar
```

## Comptes de démonstration

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin` | `Admin@2026` | Responsable des stages |
| `E2026001` … `E2026008` | `Etudiant@2026` | Élèves ingénieurs INPT (AMOA, ASEDS, DATA, ICCN, SESNum, Cloud & IoT, SUD) ; E2026005 est suspendue |
| `atlas`, `nova`, `fintech`, `cyber`, `cloud` | `Entreprise@2026` | Entreprises référencées |
| `startup` | `Entreprise@2026` | Entreprise **non** référencée |
| `systeme` | `Systeme@2026` | Compte technique de l'orchestrateur |

Les services SOAP exigent un en-tête **WS-Security UsernameToken**. Les services REST acceptent une **clé d'API**
(en-tête `X-API-Key`) ou HTTP Basic (interface web). Chaque compte a une clé de démonstration `stg_demo_<identifiant>`,
par exemple `stg_demo_admin` ; le bouton « Ma clé d'API » de l'interface en génère une nouvelle.

Données sensibles : une entreprise qui consulte un candidat voit son profil académique, mais pas son téléphone,
son e-mail (masqués) ni son adresse.

## Tests

- `mvn test` — 28 tests d'intégration de bout en bout (SOAP, REST, processus BPMN, pannes simulées).
  Sur PostgreSQL : `mvn test -Ddb.url=jdbc:postgresql://… -Dbpmn.db.url=…`.
- **Postman** : `docs/postman/gestion-stages.postman_collection.json` (39 requêtes, 45 assertions, à lancer dans l'ordre sur une base neuve).
- **SoapUI** : importer les WSDL ; requêtes prêtes à l'emploi dans `docs/soap/` (réponses obtenues dans `docs/soap/reponses/`).

## Organisation du code

```
src/main/java/com/universite/stages/
├── soap/etudiants|entreprises|offres   Services 1-3 (contrat SEI, implémentation, DAO, DTO JAXB)
├── rest/candidatures|notifications|stages  Services 4-6 (ressources JAX-RS, DAO)
├── rest/processus                      API d'orchestration (démarrage, tâches humaines, suivi)
├── bpmn/                               Moteur Flowable + délégués des tâches de service
├── clients/                            Clients SOAP dynamiques, client REST, politique de reprise
├── security/                           WS-Security, PBKDF2, rôles
└── common/                             Configuration, JDBC, validation, simulateur de pannes
src/main/resources/
├── processes/candidature-stage.bpmn20.xml   Processus BPMN 2.0 (modèle + diagramme)
├── schema.sql                               Schéma relationnel
└── public/                                  Interface web (HTML/CSS/JS, bpmn-js)
docs/       WSDL exportés, requêtes SOAP, collection Postman, script vidéo
rapport/    Rapport technique LaTeX (rapport.tex → rapport.pdf)
tools/      Génération du BPMN, rendu du diagramme, captures de l'interface
```

## Règles de gestion principales

- 5 candidatures en cours au maximum par étudiant ;
- une candidature n'est annulable que tant qu'elle est « déposée » : dès que l'entreprise clique sur « Examiner », elle passe « en cours d'évaluation » ;
- une offre convient à un étudiant si sa filière et son niveau sont acceptés et que la date limite n'est pas dépassée ;
- une place est retirée de l'offre à chaque acceptation (l'offre devient « pourvue » à zéro).

## Rapport

```bash
cd rapport && latexmk -pdf rapport.tex
```
