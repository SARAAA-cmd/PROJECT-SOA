# Script de la vidéo de démonstration (≈ 10 min)

Préparation : base neuve (supprimer le dossier `data`), lancer `mailpit`, puis `java -jar target/gestion-stages.jar`.
Ouvrir quatre onglets : l'interface (`http://localhost:8090`), Mailpit (`http://localhost:8025`), SoapUI et Postman.

| # | Durée | Écran | Ce qu'on montre | Ce qu'on dit |
|---|---|---|---|---|
| 1 | 0:45 | Schéma d'architecture du rapport | 3 services SOAP, 3 REST, orchestrateur BPMN, front | « Chaque service a son contrat, ses tables et sa sécurité ; ils ne communiquent que par SOAP/REST. » |
| 2 | 1:30 | SoapUI | Import de `/ws/etudiants?wsdl` ; `verifierEtudiant` ; puis requête sans en-tête WS-Security → `FailedAuthentication` ; `ajouterEtudiant` invalide → SOAP Fault détaillée (`docs/soap/04-…`) | Validation et sécurité côté SOAP |
| 3 | 1:00 | SoapUI | `verifierCompatibilite` (score de compétences), `consulterOffresEntreprise` (composition SOAP → SOAP) | Services « intelligents » |
| 4 | 1:30 | Postman | Lancer la collection complète (Runner) : 39 requêtes, 46 assertions vertes ; montrer l'onglet *Authorization* (clé d'API `X-API-Key`) et la requête « clé invalide → 401 » ; zoom sur 201/400/403/409/422 | Conventions REST, codes HTTP, sécurité |
| 5 | 2:00 | Interface — Karim (INE1), Omar (INE3), puis Yasmine (INE2) | Offres filtrées par type de stage selon l'année (filtre « Type de stage ») ; « Mon profil » ; offres avec score d'adéquation ; postuler à « Assistant chef de projet AMOA » → enregistrée ; postuler à « Data analyst » → rejet motivé | Démarrage du processus BPMN |
| 6 | 1:00 | Interface — Atlas Digital | Candidature « Nouvelle » → **Examiner** (passe « en cours d'évaluation », l'étudiante ne peut plus annuler) → demander un complément, puis accepter avec dates et tuteur ; montrer que le téléphone du candidat est masqué | Tâche humaine, règles, données sensibles |
| 7 | 1:00 | Mailpit + interface — Yasmine | Ouvrir Mailpit : e-mails de dépôt, de complément et d'acceptation (rendu HTML) ; puis notifications et « Mon stage » dans l'interface | Vrais e-mails et suivi |
| 8 | 1:15 | Interface — Responsable | Supervision : panne du service Candidatures ; Omar postule → incident ; Incidents → voir l'instance (diagramme surligné) → rétablir → **Relancer** | Gestion des erreurs et des indisponibilités |
| 9 | 0:30 | Interface — Responsable | Processus BPMN : instance acceptée, trace d'exécution et variables | Traçabilité |
| 10 | 0:30 | Terminal | `mvn test` → 31 tests verts | Scénarios de test automatisés |
