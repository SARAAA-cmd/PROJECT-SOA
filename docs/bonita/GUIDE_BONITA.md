# Reproduire le processus dans Bonita Studio

Le processus est exécuté dans la plateforme par le moteur BPMN 2.0 **Flowable** (l'« outil équivalent » prévu par le sujet).
Le fichier `src/main/resources/processes/candidature-stage.bpmn20.xml` est du BPMN 2.0 standard : il contient le modèle **et** le diagramme (BPMN-DI).
Ce guide explique comment le reproduire dans **Bonita Studio** si l'enseignant demande une démonstration sous Bonita.

## 1. Importer le diagramme

1. Démarrer la plateforme (`java -jar target/gestion-stages.jar`) : les services doivent répondre sur `http://localhost:8090`.
2. Dans Bonita Studio : **Fichier › Importer › BPMN 2.0** puis choisir **`docs/bonita/traitement-candidature-bonita.bpmn`**.
3. Bonita recrée le pool, les quatre couloirs, les tâches, les passerelles, les événements et le diagramme.

Ce fichier est une variante **prête pour Bonita** du processus exécuté par la plateforme : Bonita ne gérant pas les
sous-processus embarqués, le flux est mis à plat et chaque tâche de service critique porte son propre événement
d'erreur de bordure. Bonita impose une taille fixe aux tâches : leurs noms sont donc courts (« Vérifier étudiant »,
« Inscrire demande »…) et le détail des appels SOAP/REST se trouve dans la *Documentation* de chaque tâche. Il ne contient aucune extension propriétaire ; les conditions des passerelles sont en Groovy
(`etudiantEligible`, `recevable`, `decision == "ACCEPTEE"`…). Après l'import, il reste à déclarer les variables (§2),
configurer les connecteurs (§3) et les acteurs (§5) : un fichier BPMN standard ne transporte pas ces éléments propres à Bonita.

## 2. Variables du processus

Déclarer au niveau du pool (onglet *Données*) :

| Variable | Type | Rôle |
|---|---|---|
| `matricule`, `lettreMotivation`, `cvUrl` | Texte | Saisie du formulaire d'instanciation |
| `offreId`, `entrepriseId`, `candidatureId`, `stageId` | Entier long | Identifiants renvoyés par les services |
| `etudiantEligible`, `offreValide`, `recevable` | Booléen | Résultats des vérifications |
| `motifRejet`, `messageErreur`, `etapeCourante` | Texte | Motifs et gestion des incidents |
| `decision` (ACCEPTEE / REFUSEE / COMPLEMENT), `commentaireDecision` | Texte | Formulaire de la tâche « Évaluer » |
| `actionIncident` (RELANCER / ABANDONNER) | Texte | Formulaire de la tâche « Traiter l'incident » |

## 3. Connecteurs (une ligne par tâche de service)

Les appels SOAP utilisent le compte technique `systeme / Systeme@2026` (WS-Security) ; les appels REST, sa clé d'API
dans l'en-tête `X-API-Key: stg_demo_systeme`.

| Tâche BPMN | Connecteur Bonita | Appel |
|---|---|---|
| 2. Vérifier l'étudiant | *Web Service SOAP* | `http://localhost:8090/ws/etudiants` — `verifierEtudiant(matricule)` puis `consulterEtudiant` |
| 3. Vérifier l'offre et l'entreprise | *Web Service SOAP* ×3 | `/ws/offres` `consulterOffre`, `/ws/entreprises` `verifierEntreprise`, `/ws/offres` `verifierCompatibilite` |
| 4. Contrôler la candidature | SOAP + *REST GET* | `verifierDisponibilite(offreId)` ; `GET /api/candidatures?matricule=..&offreId=..` |
| Enregistrer la candidature | *REST POST* | `POST /api/candidatures` (corps JSON) → `candidatureId` |
| Notifications (×7) | *REST POST* | `POST /api/notifications` avec `type` = CONFIRMATION_DEPOT, RECEPTION_CANDIDATURE, REJET_CANDIDATURE, DEMANDE_COMPLEMENT, ACCEPTATION/REFUS, RELANCE_DECISION, INCIDENT_TRAITEMENT |
| Évaluer (prise en charge) / Valider décision | *REST PATCH* | `PATCH /api/candidatures/{id}/statut` (EN_COURS_EVALUATION à l'ouverture du dossier, puis la décision) |
| 9. Créer le dossier de stage | *REST POST* | `POST /api/stages` → `stageId` |
| Statut étudiant + place | *Web Service SOAP* ×2 | `changerStatutEtudiant(matricule, EN_STAGE)`, `reserverPlace(offreId)` |

L'en-tête WS-Security à placer dans l'enveloppe des connecteurs SOAP est donné dans `docs/soap/*.xml`.

## 4. Gestion des erreurs

- Dans chaque connecteur critique, onglet *Si le connecteur échoue* : **Lever un événement d'erreur** avec le code `ERREUR_TECHNIQUE`.
- Événement d'erreur de bordure sur la tâche → tâche « Journaliser l'incident » → tâche humaine « Traiter l'incident » (acteur *Responsable*)
  → passerelle « Action ? » : relancer l'étape en erreur ou abandonner (notification de l'étudiant).
- Pour les connecteurs de **notification**, choisir *Ignorer l'erreur et continuer* : une notification non envoyée ne doit pas bloquer le processus.

## 5. Acteurs et minuteur

- Couloir *Entreprise* : acteur « entreprise » (filtre d'acteur sur `entrepriseId`) pour la tâche « 7. Évaluer la candidature ».
- Couloir *Responsable des stages* : acteur « responsable » pour « Traiter l'incident ».
- Minuteur de bordure **non interrompant** sur « Évaluer la candidature » : durée `P3D` (3 jours), vers « Relancer l'entreprise ».
