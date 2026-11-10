package com.universite.stages;

import com.fasterxml.jackson.databind.JsonNode;
import com.universite.stages.clients.SoapClients;
import com.universite.stages.common.AppConfig;
import com.universite.stages.common.SimulateurPannes;
import com.universite.stages.soap.commun.ServiceException;
import com.universite.stages.soap.entreprises.Entreprise;
import com.universite.stages.soap.entreprises.EntrepriseService;
import com.universite.stages.soap.etudiants.Etudiant;
import com.universite.stages.soap.etudiants.EtudiantService;
import com.universite.stages.soap.offres.Compatibilite;
import com.universite.stages.soap.offres.DisponibiliteOffre;
import com.universite.stages.soap.offres.Offre;
import com.universite.stages.soap.offres.OffreService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration de bout en bout : la plateforme complète est démarrée
 * (bases H2 en mémoire) et sollicitée par de vrais appels SOAP / HTTP.
 * Les identifiants proviennent du jeu de démonstration (DonneesDemo).
 */
@DisplayName("Plateforme de gestion des stages")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class PlateformeIT {

    static final String ADMIN = "admin", ADMIN_PWD = "Admin@2026";
    static final String ETU_PWD = "Etudiant@2026", ENT_PWD = "Entreprise@2026";
    static Serveur serveur;

    @BeforeAll
    static void demarrer() throws Exception {
        AppConfig.set("server.port", "18080");
        AppConfig.set("server.base-url", "http://localhost:18080");
        // bases H2 en mémoire par défaut ; -Ddb.url=... permet de cibler PostgreSQL
        if (System.getProperty("db.url") == null) {
            AppConfig.set("db.url", "jdbc:h2:mem:stages-test;DB_CLOSE_DELAY=-1");
            AppConfig.set("bpmn.db.url", "jdbc:h2:mem:flowable-test;DB_CLOSE_DELAY=-1");
        }
        AppConfig.set("processus.delai-relance", "PT2S");
        AppConfig.set("client.delai-ms", "50");
        AppConfig.set("rappels.periode-heures", "0");
        AppConfig.set("mail.actif", "false");
        Http.base = "http://localhost:18080";
        serveur = new Serveur();
        serveur.demarrer();
    }

    @AfterAll
    static void arreter() {
        serveur.arreter();
    }

    static EtudiantService etudiants(String u, String p) {
        return SoapClients.etudiants(u, p);
    }

    static Http.Reponse soumettre(String matricule, long offreId) {
        return Http.post("/api/processus/candidatures", matricule, ETU_PWD,
                Map.of("offreId", offreId, "lettreMotivation", "Lettre de " + matricule, "cvUrl", "https://cv.example/" + matricule));
    }

    static String tache(JsonNode instance, String type) {
        for (JsonNode t : instance.get("tachesEnAttente")) {
            if (type.equals(t.get("type").asText())) {
                return t.get("id").asText();
            }
        }
        throw new AssertionError("Aucune tâche " + type + " : " + instance);
    }

    // =====================================================================
    @Nested
    @Order(1)
    @DisplayName("Services SOAP")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Soap {

        @Test
        @Order(1)
        @DisplayName("ST-01 Les trois WSDL sont publiés avec leurs opérations")
        void wsdl() {
            assertTrue(Http.get("/ws/etudiants?wsdl", null, null).brut().contains("verifierEtudiant"));
            assertTrue(Http.get("/ws/entreprises?wsdl", null, null).brut().contains("consulterOffresEntreprise"));
            assertTrue(Http.get("/ws/offres?wsdl", null, null).brut().contains("verifierCompatibilite"));
        }

        @Test
        @Order(2)
        @DisplayName("ST-02 Requête SOAP sans en-tête WS-Security rejetée (FailedAuthentication)")
        void sansSecurite() {
            Http.Reponse r = Http.postSoap("/ws/etudiants", """
                    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                      xmlns:etu="http://stages.universite.com/etudiants">
                      <soapenv:Body><etu:consulterEtudiant><matricule>E2026001</matricule></etu:consulterEtudiant></soapenv:Body>
                    </soapenv:Envelope>""");
            assertEquals(500, r.status());
            assertTrue(r.brut().contains("FailedAuthentication"));
        }

        @Test
        @Order(3)
        @DisplayName("ST-03 Ajout, consultation, modification et suppression d'un étudiant")
        void crudEtudiant() throws ServiceException {
            EtudiantService s = etudiants(ADMIN, ADMIN_PWD);
            Etudiant e = new Etudiant();
            e.setMatricule("E2026099");
            e.setNom("Test");
            e.setPrenom("Alice");
            e.setEmail("alice.test@etu.universite.example");
            e.setFiliere("AMOA");
            e.setNiveau("INE2");
            assertEquals("ACTIF", s.ajouterEtudiant(e).getStatut());
            assertEquals("Alice", s.consulterEtudiant("E2026099").getPrenom());
            e.setNiveau("INE3");
            assertEquals("INE3", s.modifierEtudiant(e).getNiveau());
            assertTrue(s.supprimerEtudiant("E2026099"));
            ServiceException ex = assertThrows(ServiceException.class, () -> s.consulterEtudiant("E2026099"));
            assertEquals(ServiceException.INTROUVABLE, ex.getFaultInfo().getCode());
        }

        @Test
        @Order(4)
        @DisplayName("ST-04 Validation : e-mail, niveau et matricule invalides renvoient une SOAP Fault détaillée")
        void validationEtudiant() {
            Etudiant e = new Etudiant();
            e.setMatricule("abc");
            e.setNom("X");
            e.setPrenom("Y");
            e.setEmail("pas-un-email");
            e.setFiliere("AMOA");
            e.setNiveau("M9");
            ServiceException ex = assertThrows(ServiceException.class, () -> etudiants(ADMIN, ADMIN_PWD).ajouterEtudiant(e));
            assertEquals(ServiceException.INVALIDE, ex.getFaultInfo().getCode());
            assertEquals(3, ex.getFaultInfo().getDetails().size());
        }

        @Test
        @Order(5)
        @DisplayName("ST-05 Matricule déjà existant : CONFLIT")
        void doublonEtudiant() throws ServiceException {
            Etudiant e = etudiants(ADMIN, ADMIN_PWD).consulterEtudiant("E2026001");
            ServiceException ex = assertThrows(ServiceException.class, () -> etudiants(ADMIN, ADMIN_PWD).ajouterEtudiant(e));
            assertEquals(ServiceException.CONFLIT, ex.getFaultInfo().getCode());
        }

        @Test
        @Order(6)
        @DisplayName("ST-06 Un étudiant ne peut consulter que sa fiche ni modifier les données")
        void accesEtudiant() throws ServiceException {
            EtudiantService s = etudiants("E2026003", ETU_PWD);
            assertEquals("Salma", s.consulterEtudiant("E2026003").getPrenom());
            assertEquals(ServiceException.ACCES_REFUSE,
                    assertThrows(ServiceException.class, () -> s.consulterEtudiant("E2026001")).getFaultInfo().getCode());
            assertEquals(ServiceException.ACCES_REFUSE,
                    assertThrows(ServiceException.class, () -> s.supprimerEtudiant("E2026003")).getFaultInfo().getCode());
        }

        @Test
        @Order(7)
        @DisplayName("ST-07 Vérification d'existence et de statut d'un étudiant")
        void verification() throws ServiceException {
            EtudiantService s = etudiants(ADMIN, ADMIN_PWD);
            assertTrue(s.verifierEtudiant("E2026003").isEligible());
            assertFalse(s.verifierEtudiant("E2026005").isEligible());
            assertFalse(s.verifierEtudiant("E9999999").isExiste());
        }

        @Test
        @Order(8)
        @DisplayName("ST-08 Entreprise : CRUD, offres proposées, suppression interdite si offres actives")
        void entreprises() throws ServiceException {
            EntrepriseService s = SoapClients.entreprises(ADMIN, ADMIN_PWD);
            Entreprise e = new Entreprise();
            e.setRaisonSociale("Test SARL");
            e.setSecteurActivite("Industrie");
            e.setEmail("contact@test.example");
            e.setContactNom("M. Test");
            e.setContactEmail("rh@test.example");
            e.setReferencee(true);
            long id = s.ajouterEntreprise(e).getId();
            assertNotNull(s.consulterEntreprise(id).getDateReferencement());
            assertTrue(s.supprimerEntreprise(id));
            assertEquals(2, s.consulterOffresEntreprise(1).size());
            assertEquals(ServiceException.REGLE_METIER,
                    assertThrows(ServiceException.class, () -> s.supprimerEntreprise(1)).getFaultInfo().getCode());
        }

        @Test
        @Order(9)
        @DisplayName("ST-09 Création d'offre : entreprise non référencée refusée, propriétaire contrôlé")
        void creationOffre() throws ServiceException {
            Offre o = new Offre();
            o.setEntrepriseId(4L);
            o.setTitre("Offre test");
            o.setDescription("Description");
            o.setDureeMois(3);
            o.setNombrePlaces(1);
            o.setDateLimite(LocalDate.now().plusDays(10));
            OffreService admin = SoapClients.offres(ADMIN, ADMIN_PWD);
            ServiceException sansType = assertThrows(ServiceException.class, () -> admin.creerOffre(o));
            assertEquals(ServiceException.INVALIDE, sansType.getFaultInfo().getCode());
            assertTrue(sansType.getFaultInfo().getDetails().stream().anyMatch(d -> d.contains("typeStage")));
            o.setTypeStage("PFE");
            assertEquals(ServiceException.REGLE_METIER,
                    assertThrows(ServiceException.class, () -> admin.creerOffre(o)).getFaultInfo().getCode());
            o.setEntrepriseId(2L);
            assertEquals(ServiceException.ACCES_REFUSE, assertThrows(ServiceException.class,
                    () -> SoapClients.offres("atlas", ENT_PWD).creerOffre(o)).getFaultInfo().getCode());
            Offre creee = SoapClients.offres("nova", ENT_PWD).creerOffre(o);
            assertEquals("ACTIVE", creee.getStatut());
            assertEquals("INE3", creee.getNiveauxAutorises());
            assertEquals(1, creee.getPlacesDisponibles());
            assertTrue(admin.supprimerOffre(creee.getId()));
        }

        @Test
        @Order(10)
        @DisplayName("ST-10 Compatibilité offre / profil : filière, type de stage / niveau, date limite et score de compétences")
        void compatibilite() throws ServiceException {
            OffreService s = SoapClients.offres(ADMIN, ADMIN_PWD);
            // offre 1 : stage ouvrier / PFA (INE1, INE2), filières AMOA et ASEDS
            Compatibilite ok = s.verifierCompatibilite(1, "AMOA", "INE2", "BPMN, uml");
            assertTrue(ok.isCompatible());
            assertEquals(50, ok.getScoreCompetences());
            assertTrue(s.verifierCompatibilite(1, "AMOA", "INE1", "").isCompatible());
            Compatibilite ko = s.verifierCompatibilite(1, "DATA", "INE3", "");
            assertFalse(ko.isCompatible());
            assertEquals(2, ko.getMotifs().size());
            assertTrue(ko.getMotifs().contains("Cette offre de stage ouvrier / PFA est réservée aux étudiants de 1re et 2e année (INE1, INE2)."));
            // offre 2 : PFE (INE3 uniquement)
            assertTrue(s.verifierCompatibilite(2, "ASEDS", "INE3", "").isNiveauCompatible());
            Compatibilite pfe = s.verifierCompatibilite(2, "ASEDS", "INE2", "");
            assertFalse(pfe.isNiveauCompatible());
            assertEquals(List.of("Cette offre de stage PFE est réservée aux étudiants de 3e année (INE3)."), pfe.getMotifs());
            // offre 9 : stage d'observation (INE1 uniquement, toutes filières)
            assertTrue(s.verifierCompatibilite(9, "SUD", "INE1", "").isCompatible());
            assertFalse(s.verifierCompatibilite(9, "SUD", "INE2", "").isNiveauCompatible());
            assertFalse(s.verifierCompatibilite(9, "SUD", "INE3", "").isNiveauCompatible());
            Compatibilite expiree = s.verifierCompatibilite(5, "AMOA", "INE2", "");
            assertFalse(expiree.isDateLimiteRespectee());
            assertFalse(expiree.isCompatible());
        }

        @Test
        @Order(11)
        @DisplayName("ST-11 Disponibilité : offre dont la date limite est dépassée")
        void disponibilite() throws ServiceException {
            DisponibiliteOffre d = SoapClients.offres(ADMIN, ADMIN_PWD).verifierDisponibilite(5);
            assertTrue(d.isDateLimiteDepassee());
            assertFalse(d.isDisponible());
        }

        @Test
        @Order(12)
        @DisplayName("ST-12 Données sensibles masquées pour une entreprise, visibles pour le responsable")
        void masquage() throws ServiceException {
            Etudiant vuEntreprise = etudiants("atlas", ENT_PWD).consulterEtudiant("E2026001");
            assertEquals("+212 6 •• •• •• 01", vuEntreprise.getTelephone());
            assertTrue(vuEntreprise.getEmail().startsWith("y•••••@"));
            assertEquals(null, vuEntreprise.getAdresse());
            assertEquals("AMOA", vuEntreprise.getFiliere());
            Etudiant vuAdmin = etudiants(ADMIN, ADMIN_PWD).consulterEtudiant("E2026001");
            assertEquals("+212 6 61 23 45 01", vuAdmin.getTelephone());
        }

        @Test
        @Order(13)
        @DisplayName("ST-13 Offres filtrées côté serveur selon le niveau : INE1 sans PFE, INE2 ouvrier / PFA, INE3 PFE")
        void filtrageParNiveau() throws ServiceException {
            java.util.function.Function<String, java.util.Set<String>> types = (m) -> {
                try {
                    return SoapClients.offres(m, ETU_PWD).listerOffresDisponibles().stream()
                            .map(Offre::getTypeStage).collect(java.util.stream.Collectors.toSet());
                } catch (ServiceException e) {
                    throw new IllegalStateException(e);
                }
            };
            assertEquals(java.util.Set.of("OBSERVATION", "OUVRIER_PFA"), types.apply("E2026004"));  // INE1
            assertEquals(java.util.Set.of("OUVRIER_PFA"), types.apply("E2026001"));                // INE2
            assertEquals(java.util.Set.of("PFE"), types.apply("E2026007"));                        // INE3
            assertTrue(SoapClients.offres("E2026007", ETU_PWD).listerOffres(null, null).stream()
                    .allMatch(o -> "PFE".equals(o.getTypeStage())));
            assertEquals(java.util.Set.of("OBSERVATION", "OUVRIER_PFA", "PFE"),
                    SoapClients.offres(ADMIN, ADMIN_PWD).listerOffresDisponibles().stream()
                            .map(Offre::getTypeStage).collect(java.util.stream.Collectors.toSet()));
        }

        @Test
        @Order(14)
        @DisplayName("ST-14 Mon profil : seuls son propre profil et les champs autorisés sont modifiables")
        void monProfil() throws ServiceException {
            // étudiant : coordonnées modifiables, identité / filière / niveau / statut en lecture seule
            EtudiantService etu = etudiants("E2026006", ETU_PWD);
            Etudiant moi = etu.consulterEtudiant("E2026006");
            moi.setTelephone("+212 6 70 00 00 06");
            moi.setCompetences("C, IoT, Rust");
            moi.setNom("Pirate");
            moi.setNiveau("INE3");
            moi.setStatut("DIPLOME");
            Etudiant apres = etu.modifierEtudiant(moi);
            assertEquals("+212 6 70 00 00 06", apres.getTelephone());
            assertEquals("C, IoT, Rust", apres.getCompetences());
            assertEquals("Bennani", apres.getNom());
            assertEquals("INE2", apres.getNiveau());
            assertEquals("ACTIF", apres.getStatut());
            Etudiant autre = etudiants(ADMIN, ADMIN_PWD).consulterEtudiant("E2026001");
            assertEquals(ServiceException.ACCES_REFUSE,
                    assertThrows(ServiceException.class, () -> etu.modifierEtudiant(autre)).getFaultInfo().getCode());
            moi.setEmail("adresse-invalide");
            assertEquals(ServiceException.INVALIDE,
                    assertThrows(ServiceException.class, () -> etu.modifierEtudiant(moi)).getFaultInfo().getCode());

            // entreprise : coordonnées et contact modifiables, raison sociale et secteur en lecture seule
            EntrepriseService nova = SoapClients.entreprises("nova", ENT_PWD);
            Entreprise profil = nova.consulterEntreprise(2);
            profil.setContactNom("Imane Lahlou");
            profil.setRaisonSociale("Autre nom");
            profil.setSecteurActivite("Autre secteur");
            Entreprise maj = nova.modifierEntreprise(profil);
            assertEquals("Imane Lahlou", maj.getContactNom());
            assertEquals("Nova Consulting", maj.getRaisonSociale());
            assertEquals("Conseil en organisation", maj.getSecteurActivite());
            assertEquals(ServiceException.ACCES_REFUSE,
                    assertThrows(ServiceException.class, () -> nova.consulterEntreprise(1)).getFaultInfo().getCode());
            Entreprise atlas = SoapClients.entreprises(ADMIN, ADMIN_PWD).consulterEntreprise(1);
            assertEquals(ServiceException.ACCES_REFUSE,
                    assertThrows(ServiceException.class, () -> nova.modifierEntreprise(atlas)).getFaultInfo().getCode());
            // les coordonnées des autres entreprises sont masquées dans la liste
            Entreprise atlasVuParNova = nova.listerEntreprises(null).stream().filter(e -> e.getId() == 1L).findFirst().orElseThrow();
            assertTrue(atlasVuParNova.getContactEmail().contains("•"));
        }
    }

    // =====================================================================
    @Nested
    @Order(2)
    @DisplayName("Services REST")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Rest {

        @Test
        @Order(1)
        @DisplayName("RT-01 401 sans authentification, 403 pour un rôle non autorisé")
        void securite() {
            assertEquals(401, Http.get("/api/candidatures", null, null).status());
            assertEquals(401, Http.get("/api/candidatures", "admin", "mauvais").status());
            assertEquals(403, Http.post("/api/stages", "E2026001", ETU_PWD, Map.of()).status());
        }

        @Test
        @Order(2)
        @DisplayName("RT-02 Candidature : création 201 + Location, validation 400, doublon 409")
        void candidature() {
            Http.Reponse invalide = Http.post("/api/candidatures", ADMIN, ADMIN_PWD, Map.of("lettreMotivation", "x"));
            assertEquals(400, invalide.status());
            assertEquals("DONNEES_INVALIDES", invalide.json().get("erreur").asText());
            Http.Reponse ok = Http.post("/api/candidatures", ADMIN, ADMIN_PWD,
                    Map.of("matricule", "E2026004", "offreId", 3, "entrepriseId", 2));
            assertEquals(201, ok.status());
            assertEquals("DEPOSEE", ok.json().get("statut").asText());
            assertEquals(409, Http.post("/api/candidatures", ADMIN, ADMIN_PWD,
                    Map.of("matricule", "E2026004", "offreId", 3)).status());
            assertEquals(400, Http.post("/api/candidatures", ADMIN, ADMIN_PWD, "{json invalide").status());
        }

        @Test
        @Order(3)
        @DisplayName("RT-03 Automate des statuts : transition interdite, annulation réservée au statut DEPOSEE, historique")
        void transitions() {
            long id = Http.get("/api/candidatures?matricule=E2026004&offreId=3", ADMIN, ADMIN_PWD).json().get(0).get("id").asLong();
            assertEquals(409, Http.appel("PATCH", "/api/candidatures/" + id + "/statut", ADMIN, ADMIN_PWD,
                    Map.of("statut", "ACCEPTEE")).status());
            assertEquals(403, Http.appel("DELETE", "/api/candidatures/" + id, "E2026001", ETU_PWD, null).status());
            assertEquals(200, Http.appel("PATCH", "/api/candidatures/" + id + "/statut", ADMIN, ADMIN_PWD,
                    Map.of("statut", "EN_COURS_EVALUATION")).status());
            assertEquals(422, Http.appel("DELETE", "/api/candidatures/" + id, "E2026004", ETU_PWD, null).status());
            assertEquals(200, Http.appel("PATCH", "/api/candidatures/" + id + "/statut", ADMIN, ADMIN_PWD,
                    Map.of("statut", "REFUSEE", "motif", "Test")).status());
            assertEquals(3, Http.get("/api/candidatures/" + id + "/historique", ADMIN, ADMIN_PWD).json().size());

            long id2 = Http.post("/api/candidatures", ADMIN, ADMIN_PWD,
                    Map.of("matricule", "E2026004", "offreId", 4)).json().get("id").asLong();
            Http.Reponse annul = Http.appel("DELETE", "/api/candidatures/" + id2, "E2026004", ETU_PWD, null);
            assertEquals("ANNULEE", annul.json().get("statut").asText());
            assertEquals(422, Http.appel("DELETE", "/api/candidatures/" + id2, "E2026004", ETU_PWD, null).status());
        }

        @Test
        @Order(4)
        @DisplayName("RT-04 Notification : gabarit rendu, périmètre du destinataire respecté")
        void notification() {
            Http.Reponse r = Http.post("/api/notifications", ADMIN, ADMIN_PWD, Map.of("type", "ACCEPTATION",
                    "destinataireType", "ETUDIANT", "destinataireId", "E2026004", "email", "karim@etu.example",
                    "parametres", Map.of("nomEtudiant", "Karim", "titreOffre", "Stage X", "entreprise", "Nova")));
            assertEquals(201, r.status());
            assertTrue(r.json().get("message").asText().contains("Félicitations Karim"));
            long id = r.json().get("id").asLong();
            assertEquals(403, Http.get("/api/notifications/" + id, "E2026001", ETU_PWD).status());
            assertEquals(200, Http.get("/api/notifications/" + id, "E2026004", ETU_PWD).status());
            assertEquals(400, Http.post("/api/notifications", ADMIN, ADMIN_PWD, Map.of("type", "INCONNU")).status());
        }

        @Test
        @Order(5)
        @DisplayName("RT-05 Stage : comptes rendus, évaluation finale qui clôture le stage, historique")
        void stage() throws ServiceException {
            Http.Reponse s = Http.post("/api/stages", ADMIN, ADMIN_PWD, Map.of("candidatureId", 9001, "matricule", "E2026004",
                    "offreId", 3, "entrepriseId", 2, "dateDebut", LocalDate.now().minusDays(10).toString(),
                    "dateFin", LocalDate.now().plusDays(80).toString()));
            assertEquals(201, s.status());
            assertEquals("EN_COURS", s.json().get("statut").asText());
            long id = s.json().get("id").asLong();
            assertEquals(409, Http.post("/api/stages", ADMIN, ADMIN_PWD, Map.of("candidatureId", 9001, "matricule", "E2026004",
                    "offreId", 3, "entrepriseId", 2, "dateDebut", "2027-01-01", "dateFin", "2027-03-01")).status());
            assertEquals(201, Http.post("/api/stages/" + id + "/comptes-rendus", "E2026004", ETU_PWD,
                    Map.of("periode", "Semaine 1", "contenu", "Prise en main du projet")).status());
            assertEquals(400, Http.post("/api/stages/" + id + "/evaluations", "nova", ENT_PWD,
                    Map.of("type", "FINALE", "evaluateur", "Tuteur", "note", 25)).status());
            assertEquals(201, Http.post("/api/stages/" + id + "/evaluations", "nova", ENT_PWD,
                    Map.of("type", "FINALE", "evaluateur", "Tuteur", "note", 16.5)).status());
            assertEquals("TERMINE", Http.get("/api/stages/" + id + "/statut", ADMIN, ADMIN_PWD).json().get("statut").asText());
            JsonNode h = Http.get("/api/stages/etudiant/E2026004/historique", "E2026004", ETU_PWD).json();
            assertEquals(16.5, h.get(0).get("moyenne").asDouble());
            assertEquals("ACTIF", etudiants(ADMIN, ADMIN_PWD).consulterEtudiant("E2026004").getStatut());
        }

        @Test
        @Order(6)
        @DisplayName("RT-06 Clé d'API : accès accepté, clé invalide refusée, régénération réservée au responsable et révocation")
        void cleApi() {
            assertEquals("RESPONSABLE", cle("/api/moi", "stg_demo_admin").json().get("role").asText());
            Http.Reponse ko = cle("/api/moi", "stg_fausse_cle");
            assertEquals(401, ko.status());
            assertEquals("CLE_API_INVALIDE", ko.json().get("erreur").asText());
            assertEquals(200, cle("/api/moi", "stg_demo_E2026008").status());
            assertEquals(403, Http.post("/api/moi/cle-api", "E2026008", ETU_PWD, Map.of()).status());
            String nouvelle = Http.post("/api/moi/cle-api", ADMIN, ADMIN_PWD, Map.of()).json().get("cleApi").asText();
            assertEquals(ADMIN, cle("/api/moi", nouvelle).json().get("username").asText());
            assertEquals(401, cle("/api/moi", "stg_demo_admin").status());
        }

        private Http.Reponse cle(String chemin, String cle) {
            try {
                var r = java.net.http.HttpClient.newHttpClient().send(
                        java.net.http.HttpRequest.newBuilder(java.net.URI.create(Http.base + chemin)).header("X-API-Key", cle).build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                return new Http.Reponse(r.statusCode(), com.universite.stages.common.Json.MAPPER.readTree(r.body()), r.body());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // =====================================================================
    @Nested
    @Order(3)
    @DisplayName("Processus BPMN")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Processus {

        @Test
        @Order(1)
        @DisplayName("PT-01 Scénario nominal : candidature acceptée, stage créé, étudiant EN_STAGE, place réservée")
        void nominal() throws ServiceException {
            Http.Reponse r = soumettre("E2026002", 2);
            assertEquals(201, r.status());
            assertEquals("EN_ATTENTE_DECISION", r.json().get("statut").asText());
            long candidatureId = r.json().get("variables").get("candidatureId").asLong();
            String tache = tache(r.json(), "evaluer");

            assertEquals(1, Http.get("/api/processus/taches", "atlas", ENT_PWD).json().size());
            assertEquals(0, Http.get("/api/processus/taches", "nova", ENT_PWD).json().size());

            Http.Reponse d = Http.post("/api/processus/taches/" + tache + "/decision", "atlas", ENT_PWD,
                    Map.of("decision", "ACCEPTEE", "commentaire", "Bienvenue", "dateDebut", "2027-02-01",
                            "dateFin", "2027-07-31", "tuteur", "N. Berrada"));
            assertEquals("ACCEPTEE", d.json().get("statut").asText());
            assertEquals("ACCEPTEE", Http.get("/api/candidatures/" + candidatureId, ADMIN, ADMIN_PWD).json().get("statut").asText());
            JsonNode stages = Http.get("/api/stages?candidatureId=" + candidatureId, ADMIN, ADMIN_PWD).json();
            assertEquals("2027-02-01", stages.get(0).get("dateDebut").asText());
            assertEquals("EN_STAGE", etudiants(ADMIN, ADMIN_PWD).consulterEtudiant("E2026002").getStatut());
            Offre o = SoapClients.offres(ADMIN, ADMIN_PWD).consulterOffre(2);
            assertEquals(0, o.getPlacesDisponibles());
            assertEquals("POURVUE", o.getStatut());
            List<String> types = Http.get("/api/notifications?candidatureId=" + candidatureId, ADMIN, ADMIN_PWD).json()
                    .findValuesAsText("type");
            assertTrue(types.containsAll(List.of("CONFIRMATION_DEPOT", "RECEPTION_CANDIDATURE", "ACCEPTATION")));
        }

        @Test
        @Order(2)
        @DisplayName("PT-02 Rejets automatiques motivés (statut, niveau, date limite, référencement, doublon, offre pourvue)")
        void rejets() {
            Map<String, Long> cas = Map.of("E2026005", 1L, "E2026003", 5L);
            cas.forEach((m, o) -> assertEquals("REJETEE", soumettre(m, o).json().get("statut").asText(), m + "/" + o));
            JsonNode nonRef = soumettre("E2026003", 6).json();
            assertTrue(nonRef.get("variables").get("motifRejet").asText().contains("non référencée"));
            JsonNode pourvue = soumettre("E2026001", 2).json();
            assertEquals("REJETEE", pourvue.get("statut").asText());
            assertEquals("finRejet", nonRef.get("evenementFin").asText());
        }

        @Test
        @Order(11)
        @DisplayName("PT-11 Type de stage : candidature rejetée par le processus avec un motif clair")
        void rejetTypeDeStage() {
            JsonNode pfe = soumettre("E2026004", 11).json();   // INE1 -> PFE
            assertEquals("REJETEE", pfe.get("statut").asText());
            assertEquals("Cette offre de stage PFE est réservée aux étudiants de 3e année (INE3).",
                    pfe.get("variables").get("motifRejet").asText());
            JsonNode obs = soumettre("E2026001", 10).json();   // INE2 -> observation
            assertEquals("REJETEE", obs.get("statut").asText());
            assertTrue(obs.get("variables").get("motifRejet").asText()
                    .contains("Cette offre de stage d'observation est réservée aux étudiants de 1re année (INE1)."));
            // INE1 -> stage d'observation : recevable, transmis à l'entreprise
            assertEquals("EN_ATTENTE_DECISION", soumettre("E2026004", 9).json().get("statut").asText());
        }

        @Test
        @Order(3)
        @DisplayName("PT-03 Demande de complément puis refus : boucle BPMN et notifications")
        void complementPuisRefus() {
            JsonNode r = soumettre("E2026001", 3).json();
            long candidatureId = r.get("variables").get("candidatureId").asLong();
            assertEquals(400, Http.post("/api/processus/taches/" + tache(r, "evaluer") + "/decision", "nova", ENT_PWD,
                    Map.of("decision", "COMPLEMENT")).status());
            JsonNode c = Http.post("/api/processus/taches/" + tache(r, "evaluer") + "/decision", "nova", ENT_PWD,
                    Map.of("decision", "COMPLEMENT", "commentaire", "Relevé de notes ?")).json();
            assertEquals("EN_ATTENTE_DECISION", c.get("statut").asText());
            assertEquals(1, c.get("variables").get("nombreDemandesComplement").asInt());
            JsonNode f = Http.post("/api/processus/taches/" + tache(c, "evaluer") + "/decision", "nova", ENT_PWD,
                    Map.of("decision", "REFUSEE", "commentaire", "Profil non retenu")).json();
            assertEquals("REFUSEE", f.get("statut").asText());
            List<String> types = Http.get("/api/notifications?candidatureId=" + candidatureId, ADMIN, ADMIN_PWD).json()
                    .findValuesAsText("type");
            assertTrue(types.containsAll(List.of("DEMANDE_COMPLEMENT", "REFUS")));
        }

        @Test
        @Order(4)
        @DisplayName("PT-04 Indisponibilité d'un service : incident, tâche du responsable, relance réussie")
        void incidentRelance() {
            SimulateurPannes.definir("candidatures", true);
            JsonNode r;
            try {
                r = soumettre("E2026003", 4).json();
            } finally {
                SimulateurPannes.definir("candidatures", false);
            }
            assertEquals("INCIDENT_TECHNIQUE", r.get("statut").asText());
            assertEquals("VERIFICATIONS", r.get("variables").get("etapeCourante").asText());
            JsonNode apres = Http.post("/api/processus/taches/" + tache(r, "traiterIncident") + "/incident", ADMIN, ADMIN_PWD,
                    Map.of("action", "RELANCER")).json();
            assertEquals("EN_ATTENTE_DECISION", apres.get("statut").asText());
        }

        @Test
        @Order(5)
        @DisplayName("PT-05 Indisponibilité persistante : abandon du traitement et information de l'étudiant")
        void incidentAbandon() {
            SimulateurPannes.definir("stages", true);
            try {
                JsonNode r = soumettre("E2026001", 1).json();
                JsonNode d = Http.post("/api/processus/taches/" + tache(r, "evaluer") + "/decision", "atlas", ENT_PWD,
                        Map.of("decision", "ACCEPTEE")).json();
                assertEquals("INCIDENT_TECHNIQUE", d.get("statut").asText());
                assertEquals("FINALISATION", d.get("variables").get("etapeCourante").asText());
                JsonNode fin = Http.post("/api/processus/taches/" + tache(d, "traiterIncident") + "/incident", ADMIN,
                        ADMIN_PWD, Map.of("action", "ABANDONNER")).json();
                assertEquals("ABANDONNEE", fin.get("statut").asText());
            } finally {
                SimulateurPannes.definir("stages", false);
            }
        }

        @Test
        @Order(6)
        @DisplayName("PT-06 Panne du service de notification : non bloquante, incident consigné")
        void notificationNonBloquante() {
            SimulateurPannes.definir("notifications", true);
            try {
                JsonNode r = soumettre("E2026006", 8).json();
                assertEquals("EN_ATTENTE_DECISION", r.get("statut").asText());
                assertTrue(r.get("variables").get("incidents").asText().contains("CONFIRMATION_DEPOT"));
            } finally {
                SimulateurPannes.definir("notifications", false);
            }
        }

        @Test
        @Order(7)
        @DisplayName("PT-07 Annulation d'une candidature déposée : le processus est interrompu")
        void annulation() {
            JsonNode r = Http.get("/api/candidatures?matricule=E2026006&offreId=8&statut=DEPOSEE",
                    ADMIN, ADMIN_PWD).json();
            long id = r.get(0).get("id").asLong();
            String pid = r.get(0).get("processInstanceId").asText();
            assertEquals(200, Http.appel("DELETE", "/api/candidatures/" + id, "E2026006", ETU_PWD, null).status());
            JsonNode inst = Http.get("/api/processus/instances/" + pid, ADMIN, ADMIN_PWD).json();
            assertEquals("INTERROMPU", inst.get("statut").asText());
        }

        @Test
        @Order(8)
        @DisplayName("PT-08 Minuteur de relance : l'entreprise est relancée sans annuler l'évaluation")
        void relanceEntreprise() throws InterruptedException {
            JsonNode r = Http.get("/api/candidatures?matricule=E2026003&offreId=4", ADMIN, ADMIN_PWD).json();
            long id = r.get(0).get("id").asLong();
            String pid = r.get(0).get("processInstanceId").asText();
            boolean relancee = false;
            for (int i = 0; i < 30 && !relancee; i++) {
                Thread.sleep(500);
                relancee = Http.get("/api/notifications?candidatureId=" + id + "&type=RELANCE_DECISION", ADMIN, ADMIN_PWD)
                        .json().size() > 0;
            }
            assertTrue(relancee, "relance attendue");
            assertEquals("EN_ATTENTE_DECISION",
                    Http.get("/api/processus/instances/" + pid, ADMIN, ADMIN_PWD).json().get("statut").asText());
        }

        @Test
        @Order(9)
        @DisplayName("PT-09 Prise en charge par l'entreprise : la candidature passe en évaluation et n'est plus annulable")
        void priseEnCharge() {
            JsonNode r = soumettre("E2026008", 7).json();
            long id = r.get("variables").get("candidatureId").asLong();
            assertEquals("DEPOSEE", Http.get("/api/candidatures/" + id, ADMIN, ADMIN_PWD).json().get("statut").asText());
            JsonNode t = Http.post("/api/processus/taches/" + tache(r, "evaluer") + "/prise-en-charge", "cyber", ENT_PWD,
                    Map.of()).json();
            assertEquals("cyber", t.get("priseEnChargePar").asText());
            assertEquals("EN_COURS_EVALUATION",
                    Http.get("/api/candidatures/" + id, ADMIN, ADMIN_PWD).json().get("statut").asText());
            assertEquals(422, Http.appel("DELETE", "/api/candidatures/" + id, "E2026008", ETU_PWD, null).status());
        }

        @Test
        @Order(10)
        @DisplayName("PT-10 Plafond de 5 candidatures en cours : la 6e est rejetée par le processus et par l'API")
        void plafond() {
            for (long offre : new long[]{1, 3, 4, 6, 7}) {
                assertEquals(201, Http.post("/api/candidatures", ADMIN, ADMIN_PWD,
                        Map.of("matricule", "E2026007", "offreId", offre)).status());
            }
            JsonNode r = soumettre("E2026007", 12).json();
            assertEquals("REJETEE", r.get("statut").asText());
            assertTrue(r.get("variables").get("motifRejet").asText().contains("Nombre maximal"));
            assertEquals(422, Http.post("/api/candidatures", ADMIN, ADMIN_PWD,
                    Map.of("matricule", "E2026007", "offreId", 2)).status());
        }
    }
}
