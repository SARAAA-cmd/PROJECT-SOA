# Script de la vidéo de démonstration — vidéo sans son (≈ 11 min)

Projet SOA « Gestion intelligente des stages et des offres de recrutement » — INPT, filière AMOA, 2026-2027.

La vidéo montre, sans commentaire oral, ce que demande l'énoncé (§6.3) : **les tests des services**, **l'exécution du
processus BPMN** et **les principaux scénarios fonctionnels**.

Pour chaque séquence :

- **Titre à l'écran** : court texte à ajouter au montage (2 à 3 s en début de séquence) ; facultatif mais recommandé,
  puisqu'il n'y a pas de son ;
- **Actions** : les clics, dans l'ordre ;
- **À voir à l'écran** : ce qui doit être lisible avant de passer à la suite (rester 2 à 3 s dessus, zoomer si besoin).

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
4. Zoom du navigateur à 110 %, notifications du système désactivées, plein écran, curseur de souris bien visible.
   Bouger la souris lentement et marquer une courte pause après chaque clic : sans son, c'est le rythme qui guide.
5. **Ne pas lancer Postman avant le parcours dans l'interface** : la collection utilise les mêmes données
   (elle est jouée à la séquence 9, sur une base neuve).

| Compte (bouton « Comptes de démonstration ») | Rôle | Utilisé pour |
|---|---|---|
| Yasmine Benali — `E2026001` | Étudiante AMOA, **INE2** | parcours principal |
| Karim Alaoui — `E2026004` | Étudiant AMOA, **INE1** | offres d'observation |
| Omar Haddad — `E2026002` | Étudiant ASEDS, **INE3** | offres PFE, candidature pendant une panne |
| Atlas Digital — `atlas` | Entreprise | examen et décision |
| Responsable des stages — `admin` | Responsable | supervision, incidents, BPMN |

Les boutons remplissent identifiant et mot de passe. Pour changer de compte : menu du profil en haut à droite →
**Se déconnecter**.

---

## 2. Déroulé

### Séquence 1 — Architecture (0:00 – 0:30)

**Titre à l'écran** : « Plateforme SOA de gestion des stages — 3 services SOAP, 3 services REST, processus BPMN »

- **Actions** : afficher la figure 2.1 du rapport (10 s), puis la page de connexion de l'interface (5 s).
- **À voir** : les six services, l'orchestrateur BPMN, les deux bases ; la page de connexion INPT.

### Séquence 2 — Services SOAP dans SoapUI (0:30 – 2:00)

**Titre à l'écran** : « Services SOAP : WSDL, sécurité WS-Security, validation, compatibilité »

1. Ouvrir le WSDL du service Étudiants.
   - **À voir** : la liste des opérations (`ajouterEtudiant`, `verifierEtudiant`…).
2. Exécuter `03-etudiants-verifierEtudiant.xml`.
   - **À voir** : `existe = true`, `eligible = false` (étudiante suspendue).
3. Exécuter `10-sans-ws-security.xml`.
   - **À voir** : la faute `FailedAuthentication`.
4. Exécuter `04-etudiants-ajouterEtudiant-invalide.xml`.
   - **À voir** : la SOAP Fault `DONNEES_INVALIDES` et la liste des champs fautifs.
5. Exécuter `11-offres-verifierCompatibilite-type-incompatible.xml`.
   - **À voir** : `niveauCompatible = false` et le motif « Cette offre de stage PFE est réservée aux étudiants de
     3e année (INE3). »
6. *(Option)* Dans `02-etudiants-consulterEtudiant.xml`, remplacer `admin` / `Admin@2026` par
   `atlas` / `Entreprise@2026`, puis exécuter.
   - **À voir** : téléphone et e-mail masqués (`+212 6 •• •• •• 01`).

### Séquence 3 — Offres selon l'année d'études (2:00 – 2:50)

**Titre à l'écran** : « Chaque étudiant ne voit que les types de stage ouverts à son année »

1. Se connecter en **Karim (INE1)**.
   - **À voir** : la phrase « Seules les offres ouvertes à votre année d'études sont affichées : 1re année (INE1) »,
     les badges « Stage d'observation (initiation) » et « Stage ouvrier / PFA », aucun PFE.
2. Cliquer sur le filtre **« Stage d'observation (initiation) »**, puis sur **« Tous les types »**.
3. Se déconnecter, se connecter en **Omar (INE3)**.
   - **À voir** : uniquement des badges « Stage PFE ».

### Séquence 4 — Dépôt de candidatures (2:50 – 4:20)

**Titre à l'écran** : « L'étudiante postule : le processus BPMN vérifie, enregistre ou rejette »

1. Se connecter en **Yasmine (INE2)**.
   - **À voir** : score d'adéquation des compétences et motifs d'incompatibilité sur les cartes.
2. **Postuler** à « Assistant chef de projet AMOA » → **Envoyer la candidature**.
   - **À voir** : « Candidature n°… enregistrée et transmise à Atlas Digital ».
3. Fermer, **Postuler** à « Data analyst – risque de crédit » → **Envoyer la candidature**.
   - **À voir** : « Candidature non recevable » et le motif (filière AMOA non ciblée).
4. **Mes candidatures** → cliquer sur la candidature « Assistant chef de projet AMOA ».
   - **À voir** : la chronologie — 1 à 3 « Terminée », 4 « Décision de l'entreprise — En cours », 5 « À venir ».
5. Fermer. Menu du profil → **Mon profil**.
   - **À voir** : matricule, nom, filière, niveau et statut en lecture seule.
6. Saisir `abc` dans **Téléphone** → **Enregistrer**.
   - **À voir** : « Certaines informations sont à corriger : Téléphone (format incorrect). »
7. Saisir `+212 6 99 88 77 66` → **Enregistrer**.
   - **À voir** : « Profil mis à jour ».

### Séquence 5 — Examen et décision de l'entreprise (4:20 – 5:50)

**Titre à l'écran** : « L'entreprise examine le dossier (tâche humaine BPMN) et rend sa décision »

1. Se connecter en **Atlas Digital** → **Candidatures à évaluer**.
   - **À voir** : la carte de Yasmine, badge « Nouvelle ».
2. **Examiner** → décision **« Demander un complément d'information »**, commentaire
   « Merci de joindre votre relevé de notes. » → **Valider la décision**.
3. Se connecter en **Yasmine** → **Mes candidatures** → la candidature.
   - **À voir** : étape 4 « Complément d'information demandé » avec le commentaire ; le bouton
     « Annuler la candidature » a disparu.
4. Modifier le lien du CV → **Enregistrer**.
5. Se connecter en **Atlas** → **Rendre une décision** → **Accepter la candidature**, tuteur « Nadia Berrada »
   → **Valider la décision**.
   - **À voir** : « Candidature acceptée — dossier de stage n°… créé ».
6. **Mes offres**.
   - **À voir** : la colonne « Type de stage ». Cliquer **+ Nouvelle offre**, montrer la liste « Type de stage »,
     fermer.

### Séquence 6 — E-mails et suivi du stage (5:50 – 6:40)

**Titre à l'écran** : « Notifications envoyées par e-mail et suivi du stage »

1. Onglet **Mailpit**.
   - **À voir** : les e-mails de dépôt, de réception (entreprise), de rejet « Data analyst », de demande de
     complément et d'acceptation. Ouvrir l'e-mail d'acceptation (mise en page HTML).
2. Interface, **Yasmine** → **Mes candidatures** → la candidature.
   - **À voir** : les 5 étapes « Terminée », jusqu'à « Stage confirmé ».
3. **Notifications**, puis **Mon stage**.
   - **À voir** : dates, tuteur, progression.

### Séquence 7 — Panne d'un service et reprise (6:40 – 8:20)

**Titre à l'écran** : « Indisponibilité d'un service : message clair pour l'étudiant, incident pour le responsable »

1. Se connecter en **Responsable** → **Supervision** → **Simuler une panne** sur **candidatures**.
   - **À voir** : badge « En panne ».
2. Se connecter en **Omar** → **Postuler** à « Développeur Java SOA » → **Envoyer la candidature**.
   - **À voir** : « Votre candidature a bien été reçue. Son traitement prend un peu plus de temps que prévu, vous
     serez notifié(e) par e-mail. » (aucun message technique).
3. Se connecter en **Responsable** → **Incidents**.
   - **À voir** : étape BPMN, service concerné « candidatures », tentatives « 3 », message d'origine.
4. **Voir l'instance**.
   - **À voir** : le diagramme BPMN, étapes parcourues et étape en cours surlignées. Fermer.
5. **Supervision** → **Rétablir** ; **Incidents** → **Relancer l'étape**.
   - **À voir** : le message de reprise, puis « Aucun incident en attente ».

### Séquence 8 — Supervision du processus BPMN (8:20 – 9:10)

**Titre à l'écran** : « Processus BPMN exécuté : modèle, instances, trace d'exécution »

1. **Processus BPMN**.
   - **À voir** : le modèle (couloirs Étudiant, Orchestrateur SOA, Entreprise partenaire, Responsable des stages ;
     passerelles ; événements d'erreur ; minuteur de relance). Zoomer quelques secondes.
2. Cliquer sur l'instance **acceptée** de Yasmine.
   - **À voir** : activités parcourues en vert, trace d'exécution horodatée, variables.
3. Menu du profil du responsable.
   - **À voir** : « Ma clé d'API » (réservée au responsable).

### Séquence 9 — Tests automatisés (9:10 – 10:40)

**Titre à l'écran** : « Tests : collection Postman et 31 tests d'intégration »

1. **Base neuve** : terminal de la plateforme → `Ctrl+C`, supprimer `data`, relancer
   `java -jar target\gestion-stages.jar` (passage à couper au montage).
2. **Postman** : ouvrir une requête, onglet *Authorization*.
   - **À voir** : clé d'API dans l'en-tête `X-API-Key`.
3. **Runner** → lancer la collection.
   - **À voir** : **39 requêtes, 46 assertions, 0 échec** ; zoomer sur quelques codes (201, 400, 401, 403, 409, 422)
     et sur « Candidature non recevable (type de stage PFE réservé aux INE3) ».
4. **Terminal** : `mvn test`.
   - **À voir** : `Tests run: 31, Failures: 0, Errors: 0` et `BUILD SUCCESS`.

### Séquence 10 — Fin (10:40 – 11:00)

**Titre à l'écran** : « Projet SOA — INPT, filière AMOA, 2026-2027 » (avec les noms du groupe)

- **Actions** : afficher la page de garde du rapport ou le tableau de bord du responsable (5 à 10 s).

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

**Conseils pour une vidéo sans son**

- Garder chaque résultat important à l'écran 2 à 3 s ; zoomer (Ctrl + molette) sur les messages et les motifs.
- Les titres de séquence peuvent être ajoutés au montage (Clipchamp, OpenShot, Shotcut…) ou affichés dans un fichier
  texte en plein écran entre deux séquences.
- Couper au montage les temps morts : changements de compte, chargements, redémarrage de la séquence 9.

**Si quelque chose ne se passe pas comme prévu**

- *Aucun e-mail dans Mailpit* : Mailpit n'était pas lancé au moment du dépôt. Les notifications restent visibles dans
  l'interface ; pour les e-mails, recommencer sur une base neuve avec Mailpit démarré.
- *« Candidature envoyée » déjà affiché sur une offre* : la base n'est pas neuve (supprimer `data` et relancer).
- *Postman en échec* : la collection doit être lancée sur une base neuve, sans parcours préalable dans l'interface.
- *Accents mal affichés dans la console Windows* : exécuter `chcp 65001` avant `java -jar` (sans effet sur l'interface).
