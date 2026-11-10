# Script de la vidéo de démonstration (≈ 13 min)

Projet SOA « Gestion intelligente des stages et des offres de recrutement » — INPT, filière AMOA, 2026-2027.

La vidéo doit montrer ce que demande l'énoncé (§6.3) : **les tests des services**, **l'exécution du processus BPMN**
et **les principaux scénarios fonctionnels**. Le déroulé suit cet ordre :
architecture → services SOAP → parcours complet dans l'interface → panne et reprise → supervision BPMN → tests automatisés.

---

## 1. Préparation (avant d'enregistrer)

1. **Base neuve** : arrêter la plateforme, supprimer le dossier `data` du projet.
2. Lancer **Mailpit** (`mailpit.exe`), puis la plateforme :
   ```cmd
   chcp 65001
   java -jar target\gestion-stages.jar
   ```
3. Ouvrir à l'avance :
   - navigateur : `http://localhost:8090` (interface) et `http://localhost:8025` (Mailpit) ;
   - **SoapUI** : projet créé à partir de `http://localhost:8090/ws/etudiants?wsdl`, `/ws/entreprises?wsdl` et
     `/ws/offres?wsdl`, avec le contenu des fichiers de `docs/soap/` collé dans les requêtes correspondantes ;
   - **Postman** : collection `docs/postman/gestion-stages.postman_collection.json` importée ;
   - le **rapport** (`rapport/rapport.pdf`) ouvert sur la figure 2.1 (architecture) ;
   - un terminal dans le dossier du projet (pour `mvn test`).
4. Zoom du navigateur à 110 %, notifications du système désactivées, fenêtre en plein écran.
5. **Ne pas lancer Postman avant le parcours dans l'interface** : la collection utilise les mêmes données
   (elle est jouée à la séquence 9, sur une base neuve).

| Compte (bouton « Comptes de démonstration ») | Rôle | Utilisé pour |
|---|---|---|
| Yasmine Benali — `E2026001` | Étudiante AMOA, **INE2** | parcours principal |
| Karim Alaoui — `E2026004` | Étudiant AMOA, **INE1** | offres d'observation |
| Omar Haddad — `E2026002` | Étudiant ASEDS, **INE3** | offres PFE, candidature pendant une panne |
| Atlas Digital — `atlas` | Entreprise | examen et décision |
| Responsable des stages — `admin` | Responsable | supervision, incidents, BPMN |

Mots de passe : `Etudiant@2026`, `Entreprise@2026`, `Admin@2026` (remplis automatiquement par les boutons).
Pour changer de compte : menu du profil en haut à droite → **Se déconnecter**.

---

## 2. Déroulé

### Séquence 1 — Présentation et architecture (0:00 – 1:00)

**Écran** : figure 2.1 du rapport, puis la page de connexion.

> « Bonjour, nous présentons notre projet SOA de gestion intelligente des stages. La plateforme comprend trois
> services SOAP — Étudiants, Entreprises et Offres de stage —, trois services REST — Candidatures, Notifications et
> Suivi des stages —, et un processus BPMN 2.0 exécuté par le moteur Flowable, qui orchestre ces six services.
> Chaque service a son contrat, ses tables et sa sécurité : ils ne communiquent que par des appels SOAP ou REST.
> Les données sont stockées dans une base relationnelle, H2 ou PostgreSQL, et une interface web aux couleurs de
> l'INPT permet à chaque acteur d'utiliser la plateforme. »

### Séquence 2 — Services SOAP dans SoapUI (1:00 – 2:30)

**Écran** : SoapUI.

1. Montrer le **WSDL** du service Étudiants et la liste de ses opérations.

   > « Chaque service SOAP est décrit par un WSDL, et chaque appel est authentifié par un en-tête WS-Security. »
2. Exécuter `03-etudiants-verifierEtudiant.xml` → l'étudiante E2026005 existe, mais elle est suspendue :
   **non éligible**.
3. Exécuter `10-sans-ws-security.xml` → **faute `FailedAuthentication`**.

   > « Sans en-tête de sécurité, la requête est refusée. »
4. Exécuter `04-etudiants-ajouterEtudiant-invalide.xml` → **SOAP Fault `DONNEES_INVALIDES`** avec la liste des
   champs fautifs.

   > « Les données sont validées : la faute SOAP indique précisément chaque champ à corriger. »
5. Exécuter `11-offres-verifierCompatibilite-type-incompatible.xml` → `niveauCompatible = false` et le motif
   « Cette offre de stage PFE est réservée aux étudiants de 3e année (INE3). »

   > « Le service Offres vérifie la compatibilité d'un étudiant avec une offre : filière, date limite et type de stage.
   > Chaque type est réservé à certaines années : observation pour les INE1, stage ouvrier ou PFA pour les INE1 et
   > INE2, PFE pour les INE3. »
6. *(Option, 15 s)* Dans `02-etudiants-consulterEtudiant.xml`, remplacer `admin` / `Admin@2026` par
   `atlas` / `Entreprise@2026` → téléphone et e-mail **masqués**.

   > « Une entreprise ne voit pas les coordonnées personnelles d'un candidat. »

### Séquence 3 — Offres selon l'année d'études (2:30 – 3:30)

**Écran** : interface.

1. Se connecter en **Karim (INE1)**.

   > « Un étudiant ne voit que les offres ouvertes à son année. Karim est en première année : il voit les stages
   > d'observation et les stages ouvriers, jamais de PFE. Ce filtrage est fait par le serveur, pas seulement par
   > l'interface. »
2. Cliquer sur le filtre **« Stage d'observation (initiation) »**, puis sur **« Tous les types »**.
3. Se déconnecter, se connecter en **Omar (INE3)** : uniquement des offres **« Stage PFE »**.

   > « Omar, en troisième année, ne voit que les projets de fin d'études. »

### Séquence 4 — Dépôt de candidatures par l'étudiante (3:30 – 5:00)

**Écran** : interface, compte **Yasmine (INE2)**.

1. Montrer la liste : badges « Stage ouvrier / PFA », **score d'adéquation**, motifs d'incompatibilité.

   > « Pour chaque offre, le service SOAP calcule un score d'adéquation des compétences et signale les
   > incompatibilités. »
2. **Postuler** à « Assistant chef de projet AMOA » → **Envoyer la candidature** → « Candidature n°… enregistrée et
   transmise à Atlas Digital ».

   > « Le dépôt déclenche le processus BPMN : vérification de l'étudiante, puis de l'offre et de l'entreprise par les
   > services SOAP, contrôle de la candidature, enregistrement par le service REST Candidatures, et notifications. »
3. **Postuler** à « Data analyst – risque de crédit » → **candidature non recevable**, motif : la filière AMOA n'est
   pas ciblée par l'offre.

   > « Si une condition n'est pas remplie, la passerelle du processus rejette la candidature avec un motif clair. »
4. **Mes candidatures** → cliquer sur la candidature AMOA → **chronologie en 5 étapes** : déposée, vérification,
   transmise (terminées), décision de l'entreprise « En cours », stage confirmé « À venir ».

   > « L'étudiante suit sa candidature dans une chronologie simple, sans vocabulaire technique. Le diagramme BPMN
   > est réservé au responsable des stages. »
5. Menu du profil → **Mon profil** : matricule, nom, filière, niveau et statut en lecture seule. Saisir `abc` dans
   le téléphone → **Enregistrer** → « Certaines informations sont à corriger : Téléphone (format incorrect). »
   Saisir un numéro valide → « Profil mis à jour ».

   > « Dans Mon profil, l'étudiante ne modifie que ses coordonnées et ses compétences, via le service SOAP
   > modifierEtudiant ; le reste est géré par l'école. »

### Séquence 5 — Examen et décision par l'entreprise (5:00 – 6:30)

**Écran** : interface, compte **Atlas Digital**.

1. **Candidatures à évaluer** : la candidature de Yasmine, statut « Nouvelle ». Cliquer **Examiner**.

   > « L'entreprise prend le dossier en charge : c'est une tâche humaine du processus BPMN. La candidature passe en
   > cours d'évaluation et l'étudiante ne peut plus l'annuler. »
2. Décision **« Demander un complément d'information »**, commentaire « Merci de joindre votre relevé de notes. »
   → **Valider la décision**.
3. Se connecter en **Yasmine** → **Mes candidatures** → la candidature : l'étape 4 devient
   **« Complément d'information demandé »**, avec le commentaire ; l'annulation n'est plus proposée.
   Modifier le lien du CV → **Enregistrer**.
4. Revenir en **Atlas** → **Rendre une décision** → **Accepter la candidature**, garder les dates proposées,
   tuteur « Nadia Berrada » → **Valider la décision** → « Candidature acceptée — dossier de stage n°… créé ».

   > « En cas d'acceptation, le processus enregistre la décision, notifie l'étudiante, crée le dossier de stage,
   > passe l'étudiante en stage et retire une place de l'offre. »
5. *(10 s)* **Mes offres** : colonne **Type de stage** ; cliquer **+ Nouvelle offre** pour montrer que l'entreprise
   choisit seulement le type de stage, puis fermer.

### Séquence 6 — E-mails et suivi du stage (6:30 – 7:30)

1. **Mailpit** : confirmation du dépôt, réception pour l'entreprise, rejet de la candidature « Data analyst »,
   demande de complément et acceptation ; ouvrir l'e-mail d'acceptation (mise en page HTML).

   > « Le service REST Notifications envoie de vrais e-mails par SMTP ; Mailpit les capture localement. »
2. Interface, **Yasmine** : la candidature affiche les **5 étapes terminées**, jusqu'à « Stage confirmé » ;
   puis **Notifications** et **Mon stage** (dates, tuteur, progression).

### Séquence 7 — Panne d'un service et reprise (7:30 – 9:30)

1. **Responsable** → **Supervision** → **Simuler une panne** du service **candidatures**.

   > « Nous simulons l'indisponibilité du service REST Candidatures. »
2. **Omar (INE3)** → **Postuler** à « Développeur Java SOA » → message :
   « Votre candidature a bien été reçue. Son traitement prend un peu plus de temps que prévu, vous serez notifié(e)
   par e-mail. »

   > « Côté étudiant, aucun message technique : ni nom de service, ni code d'erreur. »
3. **Responsable** → **Incidents** (pastille 1) : étape BPMN, activité, **service concerné : candidatures**,
   **tentatives : 3**, message d'origine.

   > « Chaque appel est retenté trois fois. Après échec, une erreur BPMN est levée, l'incident est journalisé et une
   > tâche est créée pour le responsable, qui voit, lui, le détail technique complet. »
4. **Voir l'instance** : diagramme BPMN avec les étapes parcourues et l'étape en cours surlignée ; fermer.
5. **Supervision** → **Rétablir** → **Incidents** → **Relancer l'étape** → l'instance passe
   « En attente de décision ».

   > « Une fois le service rétabli, le responsable relance l'étape : le processus reprend là où il s'était arrêté. »

### Séquence 8 — Supervision du processus (9:30 – 10:30)

1. **Processus BPMN** : le modèle, avec ses quatre couloirs (Étudiant, Orchestrateur SOA, Entreprise partenaire,
   Responsable des stages), ses sous-processus, ses événements d'erreur et le minuteur de relance.

   > « Le processus contient des tâches de service, des événements de début et de fin, des passerelles de décision et
   > des événements d'erreur pour gérer les indisponibilités. Un minuteur relance l'entreprise si elle n'a pas décidé
   > au bout de trois jours. »
2. Cliquer sur l'instance **acceptée** de Yasmine : activités parcourues en vert, trace d'exécution horodatée,
   variables.
3. *(10 s)* Menu du profil du responsable : **Ma clé d'API**, réservée au responsable.

### Séquence 9 — Tests automatisés (10:30 – 12:30)

1. **Base neuve pour Postman** : dans le terminal de la plateforme, `Ctrl+C`, supprimer `data`, relancer
   `java -jar target\gestion-stages.jar` (passage à couper au montage).
2. **Postman** : montrer l'onglet *Authorization* d'une requête (clé d'API dans l'en-tête `X-API-Key`), puis lancer
   la collection dans le **Runner** : **39 requêtes, 46 assertions vertes**. Zoomer sur quelques codes
   (201, 400, 401, 403, 409, 422) et sur « Candidature non recevable (type de stage PFE réservé aux INE3) ».

   > « La collection couvre les services REST et l'API du processus, avec les codes HTTP attendus. »
3. **Terminal** : `mvn test` → **31 tests, 0 échec** (SOAP 14, REST 6, processus 11).

   > « 31 tests d'intégration de bout en bout vérifient les services SOAP, REST et le processus, y compris les
   > pannes ; ils passent aussi sur PostgreSQL. »

### Séquence 10 — Conclusion (12:30 – 13:00)

**Écran** : page de garde du rapport ou tableau de bord du responsable.

> « En résumé : six services SOAP et REST sécurisés et documentés, un processus BPMN qui orchestre le traitement
> d'une candidature de son dépôt jusqu'à la décision finale, avec gestion des erreurs, et une interface adaptée à
> chaque acteur. Le code, le rapport LaTeX et la collection Postman sont disponibles dans le dépôt GitHub. Merci. »

---

## 3. Aide-mémoire

| Exigence de l'énoncé | Séquence |
|---|---|
| Tests des services SOAP (WSDL, validation, sécurité) | 2 |
| Tests des services REST (codes HTTP, JSON) | 9 |
| Exécution du processus BPMN (9 étapes, passerelle, tâches humaines) | 4, 5, 8 |
| Gestion des erreurs et des indisponibilités | 7 |
| Notifications | 4, 5, 6 |
| Suivi des stages | 6 |
| Interface graphique (bonus) | 3 à 8 |

**Si quelque chose ne se passe pas comme prévu**

- *Aucun e-mail dans Mailpit* : Mailpit n'était pas lancé au moment du dépôt. Les notifications restent visibles dans
  l'interface ; pour les e-mails, recommencer sur une base neuve avec Mailpit démarré.
- *« Candidature envoyée » déjà affiché sur une offre* : la base n'est pas neuve (supprimer `data` et relancer).
- *Postman en échec* : la collection doit être lancée sur une base neuve, sans parcours préalable dans l'interface.
- *Accents mal affichés dans la console Windows* : exécuter `chcp 65001` avant `java -jar` (sans effet sur l'interface).
