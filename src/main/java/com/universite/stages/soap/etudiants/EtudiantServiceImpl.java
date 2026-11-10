package com.universite.stages.soap.etudiants;

import com.universite.stages.common.Masquage;
import com.universite.stages.common.Niveau;
import com.universite.stages.common.Validation;
import com.universite.stages.security.Role;
import com.universite.stages.security.SoapSecurity;
import com.universite.stages.security.Utilisateur;
import com.universite.stages.soap.commun.Namespaces;
import com.universite.stages.soap.commun.ServiceException;
import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

import java.time.LocalDate;
import java.util.List;

/** Implémentation du service SOAP de gestion des étudiants. */
@WebService(endpointInterface = "com.universite.stages.soap.etudiants.EtudiantService",
        serviceName = "EtudiantService", portName = "EtudiantPort",
        targetNamespace = Namespaces.ETUDIANTS)
public class EtudiantServiceImpl implements EtudiantService {

    static final List<String> STATUTS = List.of(Etudiant.ACTIF, Etudiant.SUSPENDU, Etudiant.EN_STAGE, Etudiant.DIPLOME);

    @Resource
    private WebServiceContext context;

    private final EtudiantDao dao = new EtudiantDao();

    @Override
    public Etudiant ajouterEtudiant(Etudiant etudiant) throws ServiceException {
        SoapSecurity.exiger(context, Role.RESPONSABLE);
        if (etudiant == null) {
            throw ServiceException.invalide(List.of("Aucun étudiant fourni"));
        }
        if (etudiant.getStatut() == null || etudiant.getStatut().isBlank()) {
            etudiant.setStatut(Etudiant.ACTIF);
        }
        if (etudiant.getDateInscription() == null) {
            etudiant.setDateInscription(LocalDate.now());
        }
        valider(etudiant);
        if (dao.trouver(etudiant.getMatricule()).isPresent()) {
            throw ServiceException.of(ServiceException.CONFLIT,
                    "Le matricule " + etudiant.getMatricule() + " existe déjà");
        }
        if (dao.emailUtilise(etudiant.getEmail(), null)) {
            throw ServiceException.of(ServiceException.CONFLIT, "L'adresse e-mail est déjà utilisée");
        }
        dao.inserer(etudiant);
        return etudiant;
    }

    @Override
    public Etudiant consulterEtudiant(String matricule) throws ServiceException {
        Utilisateur u = SoapSecurity.utilisateur(context);
        // un étudiant ne peut consulter que sa propre fiche
        if (u.aRole(Role.ETUDIANT) && !u.refId().equals(matricule)) {
            throw ServiceException.of(ServiceException.ACCES_REFUSE, "Accès limité à votre propre fiche");
        }
        Etudiant e = charger(matricule);
        if (u.aRole(Role.ENTREPRISE)) {
            // une entreprise voit le profil académique, pas les coordonnées personnelles
            e.setTelephone(Masquage.telephone(e.getTelephone()));
            e.setEmail(Masquage.email(e.getEmail()));
            e.setAdresse(null);
        }
        return e;
    }

    @Override
    public Etudiant modifierEtudiant(Etudiant etudiant) throws ServiceException {
        Utilisateur u = SoapSecurity.exiger(context, Role.RESPONSABLE, Role.ETUDIANT);
        if (etudiant == null) {
            throw ServiceException.invalide(List.of("Aucun étudiant fourni"));
        }
        if (u.aRole(Role.ETUDIANT) && !u.refId().equals(etudiant.getMatricule())) {
            throw ServiceException.of(ServiceException.ACCES_REFUSE, "Vous ne pouvez modifier que votre propre profil");
        }
        Etudiant existant = charger(etudiant.getMatricule());
        if (u.aRole(Role.ETUDIANT)) {
            // « Mon profil » : seules les coordonnées et les compétences sont modifiables ;
            // matricule, identité, filière, niveau et statut restent gérés par l'école
            existant.setEmail(etudiant.getEmail());
            existant.setTelephone(etudiant.getTelephone());
            existant.setAdresse(etudiant.getAdresse());
            existant.setCompetences(etudiant.getCompetences());
            etudiant = existant;
        }
        if (etudiant.getStatut() == null) {
            etudiant.setStatut(existant.getStatut());
        }
        if (etudiant.getDateInscription() == null) {
            etudiant.setDateInscription(existant.getDateInscription());
        }
        valider(etudiant);
        if (dao.emailUtilise(etudiant.getEmail(), etudiant.getMatricule())) {
            throw ServiceException.of(ServiceException.CONFLIT, "L'adresse e-mail est déjà utilisée");
        }
        dao.mettreAJour(etudiant);
        return etudiant;
    }

    @Override
    public boolean supprimerEtudiant(String matricule) throws ServiceException {
        SoapSecurity.exiger(context, Role.RESPONSABLE);
        Etudiant e = charger(matricule);
        if (Etudiant.EN_STAGE.equals(e.getStatut())) {
            throw ServiceException.of(ServiceException.REGLE_METIER,
                    "Impossible de supprimer un étudiant actuellement en stage");
        }
        return dao.supprimer(matricule);
    }

    @Override
    public List<Etudiant> listerEtudiants(String filiere, String statut) throws ServiceException {
        SoapSecurity.exiger(context, Role.RESPONSABLE, Role.SYSTEME);
        return dao.lister(filiere, statut);
    }

    @Override
    public VerificationEtudiant verifierEtudiant(String matricule) throws ServiceException {
        SoapSecurity.utilisateur(context);
        if (matricule == null || matricule.isBlank()) {
            throw ServiceException.invalide(List.of("Le matricule est obligatoire"));
        }
        return dao.trouver(matricule)
                .map(e -> {
                    boolean eligible = Etudiant.ACTIF.equals(e.getStatut());
                    String msg = eligible ? "Étudiant actif, éligible aux candidatures"
                            : "Étudiant non éligible (statut " + e.getStatut() + ")";
                    return new VerificationEtudiant(matricule, true, e.getStatut(), eligible, msg);
                })
                .orElseGet(() -> new VerificationEtudiant(matricule, false, null, false,
                        "Aucun étudiant ne correspond au matricule " + matricule));
    }

    @Override
    public Etudiant changerStatutEtudiant(String matricule, String statut) throws ServiceException {
        SoapSecurity.exiger(context, Role.RESPONSABLE, Role.SYSTEME);
        Validation v = Validation.create().obligatoire("statut", statut).dansListe("statut", statut, STATUTS);
        if (!v.estValide()) {
            throw ServiceException.invalide(v.erreurs());
        }
        Etudiant e = charger(matricule);
        e.setStatut(statut);
        dao.mettreAJour(e);
        return e;
    }

    private Etudiant charger(String matricule) throws ServiceException {
        if (matricule == null || matricule.isBlank()) {
            throw ServiceException.invalide(List.of("Le matricule est obligatoire"));
        }
        return dao.trouver(matricule)
                .orElseThrow(() -> ServiceException.introuvable("Étudiant " + matricule + " introuvable"));
    }

    private static void valider(Etudiant e) throws ServiceException {
        Validation v = Validation.create()
                .obligatoire("matricule", e.getMatricule())
                .format("matricule", e.getMatricule(), Validation.MATRICULE, "lettres majuscules suivies de chiffres, ex. E2026001")
                .obligatoire("nom", e.getNom())
                .longueurMax("nom", e.getNom(), 80)
                .obligatoire("prenom", e.getPrenom())
                .longueurMax("prenom", e.getPrenom(), 80)
                .obligatoire("email", e.getEmail())
                .email("email", e.getEmail())
                .telephone("telephone", e.getTelephone())
                .obligatoire("filiere", e.getFiliere())
                .obligatoire("niveau", e.getNiveau())
                .dansListe("niveau", e.getNiveau(), Niveau.valeurs())
                .dansListe("statut", e.getStatut(), STATUTS)
                .longueurMax("competences", e.getCompetences(), 500);
        if (!v.estValide()) {
            throw ServiceException.invalide(v.erreurs());
        }
    }
}
