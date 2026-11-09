package com.universite.stages;

import com.universite.stages.common.AppConfig;
import com.universite.stages.security.Role;
import com.universite.stages.security.UtilisateurDao;
import com.universite.stages.soap.entreprises.Entreprise;
import com.universite.stages.soap.entreprises.EntrepriseDao;
import com.universite.stages.soap.etudiants.Etudiant;
import com.universite.stages.soap.etudiants.EtudiantDao;
import com.universite.stages.soap.offres.Offre;
import com.universite.stages.soap.offres.OffreDao;

import java.time.LocalDate;
import java.util.logging.Logger;

/** Jeu de données de démonstration chargé au premier démarrage. */
final class DonneesDemo {

    private static final Logger LOG = Logger.getLogger(DonneesDemo.class.getName());

    private DonneesDemo() {
    }

    static void charger() {
        if (!UtilisateurDao.aucunUtilisateur()) {
            return;
        }
        LOG.info("Chargement des données de démonstration");
        compte("admin", "Admin@2026", Role.RESPONSABLE, null);
        UtilisateurDao.creer("systeme", AppConfig.get("systeme.password"), Role.SYSTEME, null);
        UtilisateurDao.definirCle("systeme", AppConfig.get("systeme.api-key"));

        // Élèves ingénieurs (INE1 = 1re année, INE2 = 2e année, INE3 = 3e année)
        EtudiantDao etudiants = new EtudiantDao();
        etudiant(etudiants, "E2026001", "Benali", "Yasmine", "AMOA", "INE2", "BPMN, UML, SQL, Java, Gestion de projet", Etudiant.ACTIF, "Rabat");
        etudiant(etudiants, "E2026002", "Haddad", "Omar", "ASEDS", "INE3", "Java, Spring, SOAP, REST, Docker", Etudiant.ACTIF, "Casablanca");
        etudiant(etudiants, "E2026003", "El Amrani", "Salma", "DATA", "INE2", "Python, SQL, Machine Learning", Etudiant.ACTIF, "Fès");
        etudiant(etudiants, "E2026004", "Alaoui", "Karim", "AMOA", "INE1", "UML, SQL, Merise", Etudiant.ACTIF, "Meknès");
        etudiant(etudiants, "E2026005", "Tazi", "Imane", "ICCN", "INE3", "Réseaux, Sécurité, Linux", Etudiant.SUSPENDU, "Tanger");
        etudiant(etudiants, "E2026006", "Bennani", "Youssef", "SESNum", "INE2", "C, Systèmes embarqués, IoT, Python", Etudiant.ACTIF, "Oujda");
        etudiant(etudiants, "E2026007", "Chraibi", "Hiba", "Cloud & IoT", "INE3", "AWS, Kubernetes, Docker, Python", Etudiant.ACTIF, "Kénitra");
        etudiant(etudiants, "E2026008", "Ouazzani", "Mehdi", "SUD", "INE2", "Java, Systèmes distribués, Kafka, Linux", Etudiant.ACTIF, "Agadir");
        for (int i = 1; i <= 8; i++) {
            String m = "E202600" + i;
            compte(m, "Etudiant@2026", Role.ETUDIANT, m);
        }

        // Entreprises fictives
        EntrepriseDao entreprises = new EntrepriseDao();
        long atlas = entreprise(entreprises, "Atlas Digital", "Conseil IT", "Casablanca", "Casanearshore, Sidi Maârouf",
                "Nadia Berrada", "rh@atlas-digital.example", true);
        long nova = entreprise(entreprises, "Nova Consulting", "Conseil en organisation", "Rabat", "Avenue de France, Agdal",
                "Rachid Lahlou", "stages@nova-consulting.example", true);
        long fin = entreprise(entreprises, "Maghreb FinTech", "Banque et finance", "Casablanca", "Casablanca Finance City",
                "Sara El Idrissi", "talents@maghreb-fintech.example", true);
        long nonRef = entreprise(entreprises, "StartUp Sans Convention", "E-commerce", "Marrakech", "Guéliz",
                "Ali Tazi", "contact@startup-sans-convention.example", false);
        long cyber = entreprise(entreprises, "Souss Cyber Defense", "Cybersécurité", "Agadir", "Technopole d'Agadir",
                "Othmane Amrani", "recrutement@souss-cyber.example", true);
        long cloud = entreprise(entreprises, "Sahara Cloud Services", "Cloud et hébergement", "Rabat", "Technopolis, Sala Al Jadida",
                "Leila Benjelloun", "jobs@sahara-cloud.example", true);
        compte("atlas", "Entreprise@2026", Role.ENTREPRISE, String.valueOf(atlas));
        compte("nova", "Entreprise@2026", Role.ENTREPRISE, String.valueOf(nova));
        compte("fintech", "Entreprise@2026", Role.ENTREPRISE, String.valueOf(fin));
        compte("startup", "Entreprise@2026", Role.ENTREPRISE, String.valueOf(nonRef));
        compte("cyber", "Entreprise@2026", Role.ENTREPRISE, String.valueOf(cyber));
        compte("cloud", "Entreprise@2026", Role.ENTREPRISE, String.valueOf(cloud));

        OffreDao offres = new OffreDao();
        LocalDate j = LocalDate.now();
        offre(offres, atlas, "Assistant chef de projet AMOA", "Recueil des besoins, rédaction des spécifications fonctionnelles et "
                + "modélisation BPMN des processus métier d'un client bancaire.", "BPMN, UML, SQL, Gestion de projet",
                6, j.plusDays(30), 2, "AMOA, ASEDS", "INE2", "Casablanca", Offre.ACTIVE);
        offre(offres, atlas, "Développeur Java SOA", "Développement de services web SOAP/REST et intégration dans une "
                + "architecture orientée services.", "Java, SOAP, REST, Docker", 6, j.plusDays(2), 1,
                "ASEDS", "INE3", "Casablanca", Offre.ACTIVE);
        offre(offres, nova, "Consultant junior en transformation digitale", "Accompagnement de la refonte des processus "
                + "RH d'une administration : cartographie, indicateurs et conduite du changement.", "BPMN, Gestion de projet, Excel",
                4, j.plusDays(20), 3, "AMOA", "INE1", "Rabat", Offre.ACTIVE);
        offre(offres, fin, "Data analyst – risque de crédit", "Analyse des données de crédit et construction de "
                + "tableaux de bord de suivi du risque.", "Python, SQL, Power BI", 5, j.plusDays(15), 1,
                "DATA", "INE2", "Casablanca", Offre.ACTIVE);
        offre(offres, fin, "Business analyst (candidatures closes)", "Offre dont la date limite est dépassée.",
                "SQL, UML", 3, j.minusDays(5), 2, "", null, "Casablanca", Offre.ACTIVE);
        offre(offres, nonRef, "Stage marketing digital", "Offre d'une entreprise non référencée par l'école.",
                "SEO", 3, j.plusDays(25), 1, "", null, "Marrakech", Offre.ACTIVE);
        offre(offres, cyber, "Analyste SOC junior", "Surveillance des événements de sécurité, analyse d'alertes et "
                + "rédaction de rapports d'incidents au sein d'un centre opérationnel de sécurité.", "Réseaux, Sécurité, Linux, SIEM",
                6, j.plusDays(25), 2, "ICCN, SUD", "INE2", "Agadir", Offre.ACTIVE);
        offre(offres, cloud, "Ingénieur Cloud & IoT – plateforme de télémétrie", "Conception d'une chaîne de collecte de "
                + "données de capteurs et déploiement sur Kubernetes.", "Python, Docker, Kubernetes, IoT",
                6, j.plusDays(18), 2, "Cloud & IoT, SESNum", "INE2", "Rabat", Offre.ACTIVE);
    }

    /** Crée un compte et sa clé d'API de démonstration : stg_demo_<identifiant>. */
    private static void compte(String username, String password, String role, String refId) {
        UtilisateurDao.creer(username, password, role, refId);
        UtilisateurDao.definirCle(username, "stg_demo_" + username);
    }

    private static void etudiant(EtudiantDao dao, String m, String nom, String prenom, String filiere, String niveau,
                                 String comp, String statut, String ville) {
        Etudiant e = new Etudiant();
        e.setMatricule(m);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setEmail(prenom.toLowerCase() + "." + nom.toLowerCase().replace(" ", "") + "@etu.inpt-stages.example");
        e.setTelephone("+212 6 61 23 45 0" + m.charAt(m.length() - 1));
        e.setAdresse("Résidence étudiante, " + ville);
        e.setFiliere(filiere);
        e.setNiveau(niveau);
        e.setCompetences(comp);
        e.setStatut(statut);
        e.setDateInscription(LocalDate.of(2024, 9, 15));
        dao.inserer(e);
    }

    private static long entreprise(EntrepriseDao dao, String nom, String secteur, String ville, String adresse,
                                   String contact, String email, boolean ref) {
        Entreprise e = new Entreprise();
        e.setRaisonSociale(nom);
        e.setSecteurActivite(secteur);
        e.setVille(ville);
        e.setAdresse(adresse);
        e.setEmail(email);
        e.setTelephone("+212 5 22 00 00 00");
        e.setContactNom(contact);
        e.setContactFonction("Responsable recrutement");
        e.setContactEmail(email);
        e.setReferencee(ref);
        e.setDateReferencement(ref ? LocalDate.of(2025, 1, 10) : null);
        return dao.inserer(e).getId();
    }

    private static void offre(OffreDao dao, long ent, String titre, String desc, String comp, int duree, LocalDate limite,
                              int places, String filieres, String niveau, String lieu, String statut) {
        Offre o = new Offre();
        o.setEntrepriseId(ent);
        o.setTitre(titre);
        o.setDescription(desc);
        o.setCompetencesRequises(comp);
        o.setDureeMois(duree);
        o.setDateLimite(limite);
        o.setNombrePlaces(places);
        o.setPlacesDisponibles(places);
        o.setFilieresCiblees(filieres);
        o.setNiveauMinimum(niveau);
        o.setLieu(lieu);
        o.setStatut(statut);
        o.setDatePublication(LocalDate.now().minusDays(10));
        dao.inserer(o);
    }
}
