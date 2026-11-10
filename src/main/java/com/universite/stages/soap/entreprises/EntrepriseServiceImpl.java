package com.universite.stages.soap.entreprises;

import com.universite.stages.common.Masquage;
import com.universite.stages.clients.SoapClients;
import com.universite.stages.common.Validation;
import com.universite.stages.security.Role;
import com.universite.stages.security.SoapSecurity;
import com.universite.stages.security.Utilisateur;
import com.universite.stages.soap.commun.Namespaces;
import com.universite.stages.soap.commun.ServiceException;
import com.universite.stages.soap.offres.Offre;
import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;

import java.time.LocalDate;
import java.util.List;

/** Implémentation du service SOAP de gestion des entreprises. */
@WebService(endpointInterface = "com.universite.stages.soap.entreprises.EntrepriseService",
        serviceName = "EntrepriseService", portName = "EntreprisePort",
        targetNamespace = Namespaces.ENTREPRISES)
public class EntrepriseServiceImpl implements EntrepriseService {

    @Resource
    private WebServiceContext context;

    private final EntrepriseDao dao = new EntrepriseDao();

    @Override
    public Entreprise ajouterEntreprise(Entreprise entreprise) throws ServiceException {
        SoapSecurity.exiger(context, Role.RESPONSABLE);
        if (entreprise == null) {
            throw ServiceException.invalide(List.of("Aucune entreprise fournie"));
        }
        valider(entreprise);
        if (dao.raisonSocialeUtilisee(entreprise.getRaisonSociale(), null)) {
            throw ServiceException.of(ServiceException.CONFLIT,
                    "Une entreprise nommée « " + entreprise.getRaisonSociale() + " » existe déjà");
        }
        if (entreprise.isReferencee() && entreprise.getDateReferencement() == null) {
            entreprise.setDateReferencement(LocalDate.now());
        }
        entreprise.setId(null);
        return dao.inserer(entreprise);
    }

    @Override
    public Entreprise consulterEntreprise(long entrepriseId) throws ServiceException {
        Utilisateur u = SoapSecurity.utilisateur(context);
        if (u.aRole(Role.ENTREPRISE) && !String.valueOf(entrepriseId).equals(u.refId())) {
            throw ServiceException.of(ServiceException.ACCES_REFUSE, "Vous ne pouvez consulter que le profil de votre entreprise");
        }
        return vuePour(u, charger(entrepriseId));
    }

    /**
     * Coordonnées d'une entreprise : complètes pour elle-même, le responsable et l'orchestrateur ;
     * masquées pour les autres utilisateurs (seules la raison sociale, le secteur et la ville restent lisibles).
     */
    private static Entreprise vuePour(Utilisateur u, Entreprise e) {
        boolean complete = u.aRole(Role.RESPONSABLE) || u.aRole(Role.SYSTEME)
                || (u.aRole(Role.ENTREPRISE) && String.valueOf(e.getId()).equals(u.refId()));
        if (!complete) {
            e.setAdresse(null);
            e.setEmail(Masquage.email(e.getEmail()));
            e.setTelephone(Masquage.telephone(e.getTelephone()));
            e.setContactEmail(Masquage.email(e.getContactEmail()));
            e.setContactTelephone(Masquage.telephone(e.getContactTelephone()));
        }
        return e;
    }

    @Override
    public Entreprise modifierEntreprise(Entreprise entreprise) throws ServiceException {
        Utilisateur u = SoapSecurity.exiger(context, Role.RESPONSABLE, Role.ENTREPRISE);
        if (entreprise == null || entreprise.getId() == null) {
            throw ServiceException.invalide(List.of("L'identifiant de l'entreprise est obligatoire"));
        }
        Entreprise existant = charger(entreprise.getId());
        if (u.aRole(Role.ENTREPRISE)) {
            if (!String.valueOf(entreprise.getId()).equals(u.refId())) {
                throw ServiceException.of(ServiceException.ACCES_REFUSE, "Vous ne pouvez modifier que votre entreprise");
            }
            // seul le responsable des stages peut changer le référencement, la raison sociale et le secteur
            entreprise.setReferencee(existant.isReferencee());
            entreprise.setDateReferencement(existant.getDateReferencement());
            entreprise.setRaisonSociale(existant.getRaisonSociale());
            entreprise.setSecteurActivite(existant.getSecteurActivite());
        }
        valider(entreprise);
        if (dao.raisonSocialeUtilisee(entreprise.getRaisonSociale(), entreprise.getId())) {
            throw ServiceException.of(ServiceException.CONFLIT, "Cette raison sociale est déjà utilisée");
        }
        if (entreprise.isReferencee() && entreprise.getDateReferencement() == null) {
            entreprise.setDateReferencement(LocalDate.now());
        }
        dao.mettreAJour(entreprise);
        return entreprise;
    }

    @Override
    public boolean supprimerEntreprise(long entrepriseId) throws ServiceException {
        SoapSecurity.exiger(context, Role.RESPONSABLE);
        charger(entrepriseId);
        long actives = offresDe(entrepriseId).stream().filter(o -> Offre.ACTIVE.equals(o.getStatut())).count();
        if (actives > 0) {
            throw ServiceException.of(ServiceException.REGLE_METIER,
                    "Suppression impossible : l'entreprise a " + actives + " offre(s) active(s)");
        }
        return dao.supprimer(entrepriseId);
    }

    @Override
    public List<Entreprise> listerEntreprises(String secteurActivite) throws ServiceException {
        Utilisateur u = SoapSecurity.utilisateur(context);
        return dao.lister(secteurActivite).stream().map(e -> vuePour(u, e)).toList();
    }

    @Override
    public VerificationEntreprise verifierEntreprise(long entrepriseId) throws ServiceException {
        SoapSecurity.utilisateur(context);
        return dao.trouver(entrepriseId)
                .map(e -> new VerificationEntreprise(entrepriseId, true, e.isReferencee(), e.getRaisonSociale(),
                        e.isReferencee() ? "Entreprise référencée" : "Entreprise non référencée par l'université"))
                .orElseGet(() -> new VerificationEntreprise(entrepriseId, false, false, null,
                        "Aucune entreprise ne correspond à l'identifiant " + entrepriseId));
    }

    @Override
    public List<Offre> consulterOffresEntreprise(long entrepriseId) throws ServiceException {
        SoapSecurity.utilisateur(context);
        charger(entrepriseId);
        return offresDe(entrepriseId);
    }

    /** Composition de services : les offres appartiennent au service 3, interrogé en SOAP. */
    private static List<Offre> offresDe(long entrepriseId) throws ServiceException {
        try {
            return SoapClients.offres().listerOffres(null, entrepriseId);
        } catch (WebServiceException e) {
            throw ServiceException.of(ServiceException.TECHNIQUE, "Service des offres indisponible");
        }
    }

    private Entreprise charger(long id) throws ServiceException {
        return dao.trouver(id).orElseThrow(() -> ServiceException.introuvable("Entreprise " + id + " introuvable"));
    }

    private static void valider(Entreprise e) throws ServiceException {
        Validation v = Validation.create()
                .obligatoire("raisonSociale", e.getRaisonSociale())
                .longueurMax("raisonSociale", e.getRaisonSociale(), 120)
                .obligatoire("secteurActivite", e.getSecteurActivite())
                .obligatoire("email", e.getEmail())
                .email("email", e.getEmail())
                .telephone("telephone", e.getTelephone())
                .obligatoire("contactNom", e.getContactNom())
                .obligatoire("contactEmail", e.getContactEmail())
                .email("contactEmail", e.getContactEmail())
                .telephone("contactTelephone", e.getContactTelephone());
        if (!v.estValide()) {
            throw ServiceException.invalide(v.erreurs());
        }
    }
}
