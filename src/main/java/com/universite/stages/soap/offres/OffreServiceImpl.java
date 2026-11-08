package com.universite.stages.soap.offres;

import com.universite.stages.clients.SoapClients;
import com.universite.stages.common.Niveau;
import com.universite.stages.common.Validation;
import com.universite.stages.security.Role;
import com.universite.stages.security.SoapSecurity;
import com.universite.stages.security.Utilisateur;
import com.universite.stages.soap.commun.Namespaces;
import com.universite.stages.soap.commun.ServiceException;
import com.universite.stages.soap.entreprises.VerificationEntreprise;
import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Implémentation du service SOAP de gestion des offres de stage. */
@WebService(endpointInterface = "com.universite.stages.soap.offres.OffreService",
        serviceName = "OffreService", portName = "OffrePort",
        targetNamespace = Namespaces.OFFRES)
public class OffreServiceImpl implements OffreService {

    static final List<String> STATUTS = List.of(Offre.ACTIVE, Offre.SUSPENDUE, Offre.POURVUE, Offre.CLOTUREE);

    @Resource
    private WebServiceContext context;

    private final OffreDao dao = new OffreDao();

    @Override
    public Offre creerOffre(Offre offre) throws ServiceException {
        Utilisateur u = SoapSecurity.exiger(context, Role.RESPONSABLE, Role.ENTREPRISE);
        if (offre == null) {
            throw ServiceException.invalide(List.of("Aucune offre fournie"));
        }
        verifierProprietaire(u, offre.getEntrepriseId());
        offre.setId(null);
        offre.setStatut(Offre.ACTIVE);
        offre.setDatePublication(LocalDate.now());
        offre.setPlacesDisponibles(offre.getNombrePlaces());
        valider(offre, true);
        verifierEntrepriseReferencee(offre.getEntrepriseId());
        return dao.inserer(offre);
    }

    @Override
    public Offre consulterOffre(long offreId) throws ServiceException {
        SoapSecurity.utilisateur(context);
        return charger(offreId);
    }

    @Override
    public Offre modifierOffre(Offre offre) throws ServiceException {
        Utilisateur u = SoapSecurity.exiger(context, Role.RESPONSABLE, Role.ENTREPRISE);
        if (offre == null || offre.getId() == null) {
            throw ServiceException.invalide(List.of("L'identifiant de l'offre est obligatoire"));
        }
        Offre existante = charger(offre.getId());
        verifierProprietaire(u, existante.getEntrepriseId());
        // l'entreprise propriétaire et la date de publication ne changent pas
        offre.setEntrepriseId(existante.getEntrepriseId());
        offre.setDatePublication(existante.getDatePublication());
        if (offre.getStatut() == null) {
            offre.setStatut(existante.getStatut());
        }
        int placesAttribuees = existante.getNombrePlaces() - existante.getPlacesDisponibles();
        if (offre.getNombrePlaces() < placesAttribuees) {
            throw ServiceException.of(ServiceException.REGLE_METIER,
                    "Le nombre de places ne peut être inférieur aux " + placesAttribuees + " place(s) déjà attribuée(s)");
        }
        offre.setPlacesDisponibles(offre.getNombrePlaces() - placesAttribuees);
        if (offre.getPlacesDisponibles() == 0 && Offre.ACTIVE.equals(offre.getStatut())) {
            offre.setStatut(Offre.POURVUE);
        }
        valider(offre, false);
        dao.mettreAJour(offre);
        return offre;
    }

    @Override
    public boolean supprimerOffre(long offreId) throws ServiceException {
        Utilisateur u = SoapSecurity.exiger(context, Role.RESPONSABLE, Role.ENTREPRISE);
        Offre o = charger(offreId);
        verifierProprietaire(u, o.getEntrepriseId());
        if (o.getPlacesDisponibles() < o.getNombrePlaces()) {
            throw ServiceException.of(ServiceException.REGLE_METIER,
                    "Suppression impossible : des stagiaires ont déjà été retenus ; clôturez l'offre à la place");
        }
        return dao.supprimer(offreId);
    }

    @Override
    public List<Offre> listerOffres(String statut, Long entrepriseId) throws ServiceException {
        SoapSecurity.utilisateur(context);
        return dao.lister(statut, entrepriseId);
    }

    @Override
    public List<Offre> listerOffresDisponibles() throws ServiceException {
        SoapSecurity.utilisateur(context);
        return dao.disponibles();
    }

    @Override
    public DisponibiliteOffre verifierDisponibilite(long offreId) throws ServiceException {
        SoapSecurity.utilisateur(context);
        DisponibiliteOffre d = new DisponibiliteOffre();
        d.setOffreId(offreId);
        var opt = dao.trouver(offreId);
        if (opt.isEmpty()) {
            d.setMessage("Offre " + offreId + " introuvable");
            return d;
        }
        Offre o = opt.get();
        d.setExiste(true);
        d.setActive(Offre.ACTIVE.equals(o.getStatut()));
        d.setDateLimiteDepassee(LocalDate.now().isAfter(o.getDateLimite()));
        d.setPlacesDisponibles(o.getPlacesDisponibles());
        d.setDisponible(d.isActive() && !d.isDateLimiteDepassee() && o.getPlacesDisponibles() > 0);
        if (d.isDisponible()) {
            d.setMessage("Offre disponible (" + o.getPlacesDisponibles() + " place(s))");
        } else if (!d.isActive()) {
            d.setMessage("Offre non active (statut " + o.getStatut() + ")");
        } else if (d.isDateLimiteDepassee()) {
            d.setMessage("Date limite de candidature dépassée (" + o.getDateLimite() + ")");
        } else {
            d.setMessage("Plus aucune place disponible");
        }
        return d;
    }

    @Override
    public Compatibilite verifierCompatibilite(long offreId, String filiere, String niveau, String competences)
            throws ServiceException {
        SoapSecurity.utilisateur(context);
        Validation v = Validation.create()
                .obligatoire("filiere", filiere)
                .obligatoire("niveau", niveau)
                .dansListe("niveau", niveau, Niveau.valeurs());
        if (!v.estValide()) {
            throw ServiceException.invalide(v.erreurs());
        }
        Offre o = charger(offreId);
        Compatibilite c = new Compatibilite();
        c.setOffreId(offreId);

        Set<String> filieres = decouper(o.getFilieresCiblees());
        c.setFiliereCompatible(filieres.isEmpty() || filieres.contains(normaliser(filiere)));
        if (!c.isFiliereCompatible()) {
            c.getMotifs().add("Filière « " + filiere + " » non ciblée par l'offre (" + o.getFilieresCiblees() + ")");
        }
        c.setNiveauCompatible(Niveau.atteint(niveau, o.getNiveauMinimum()));
        if (!c.isNiveauCompatible()) {
            c.getMotifs().add("Niveau " + niveau + " inférieur au niveau minimum requis " + o.getNiveauMinimum());
        }

        c.setDateLimiteRespectee(!LocalDate.now().isAfter(o.getDateLimite()));
        if (!c.isDateLimiteRespectee()) {
            c.getMotifs().add("Date limite de candidature dépassée (" + o.getDateLimite() + ")");
        }

        // score indicatif de couverture des compétences (non bloquant)
        Set<String> requises = decouper(o.getCompetencesRequises());
        Set<String> possedees = decouper(competences);
        if (requises.isEmpty()) {
            c.setScoreCompetences(100);
        } else {
            List<String> manquantes = Arrays.stream(o.getCompetencesRequises().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty() && !possedees.contains(normaliser(s))).toList();
            c.setCompetencesManquantes(manquantes);
            c.setScoreCompetences((int) Math.round(100.0 * (requises.size() - manquantes.size()) / requises.size()));
        }
        c.setCompatible(c.isFiliereCompatible() && c.isNiveauCompatible() && c.isDateLimiteRespectee());
        return c;
    }

    @Override
    public Offre reserverPlace(long offreId) throws ServiceException {
        SoapSecurity.exiger(context, Role.RESPONSABLE, Role.SYSTEME);
        Offre o = charger(offreId);
        if (!dao.decrementerPlace(offreId)) {
            throw ServiceException.of(ServiceException.REGLE_METIER,
                    "Aucune place disponible pour l'offre " + o.getTitre());
        }
        return charger(offreId);
    }

    @Override
    public List<Offre> listerOffresExpirantBientot(int jours) throws ServiceException {
        SoapSecurity.utilisateur(context);
        if (jours < 0 || jours > 365) {
            throw ServiceException.invalide(List.of("Le nombre de jours doit être compris entre 0 et 365"));
        }
        return dao.expirantAvant(LocalDate.now().plusDays(jours));
    }

    private void verifierProprietaire(Utilisateur u, Long entrepriseId) throws ServiceException {
        if (u.aRole(Role.ENTREPRISE) && (entrepriseId == null || !String.valueOf(entrepriseId).equals(u.refId()))) {
            throw ServiceException.of(ServiceException.ACCES_REFUSE,
                    "Une entreprise ne peut gérer que ses propres offres");
        }
    }

    /** Composition de services : appel SOAP au service Entreprises. */
    private static void verifierEntrepriseReferencee(long entrepriseId) throws ServiceException {
        VerificationEntreprise ve;
        try {
            ve = SoapClients.entreprises().verifierEntreprise(entrepriseId);
        } catch (WebServiceException e) {
            throw ServiceException.of(ServiceException.TECHNIQUE, "Service des entreprises indisponible");
        }
        if (!ve.isExiste() || !ve.isReferencee()) {
            throw ServiceException.of(ServiceException.REGLE_METIER, ve.getMessage());
        }
    }

    private Offre charger(long id) throws ServiceException {
        return dao.trouver(id).orElseThrow(() -> ServiceException.introuvable("Offre " + id + " introuvable"));
    }

    private static void valider(Offre o, boolean creation) throws ServiceException {
        Validation v = Validation.create()
                .obligatoire("entrepriseId", o.getEntrepriseId())
                .obligatoire("titre", o.getTitre())
                .longueurMax("titre", o.getTitre(), 150)
                .obligatoire("description", o.getDescription())
                .longueurMax("description", o.getDescription(), 2000)
                .positif("dureeMois", o.getDureeMois())
                .entre("dureeMois", o.getDureeMois(), 1, 12)
                .obligatoire("dateLimite", o.getDateLimite())
                .positif("nombrePlaces", o.getNombrePlaces())
                .dansListe("niveauMinimum", blankToNull(o.getNiveauMinimum()), Niveau.valeurs())
                .dansListe("statut", o.getStatut(), STATUTS);
        if (creation) {
            v.dateFuture("dateLimite", o.getDateLimite());
        }
        if (!v.estValide()) {
            throw ServiceException.invalide(v.erreurs());
        }
        o.setNiveauMinimum(blankToNull(o.getNiveauMinimum()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Set<String> decouper(String liste) {
        if (liste == null || liste.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(liste.split(",")).map(OffreServiceImpl::normaliser)
                .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    /** Comparaison insensible à la casse et aux accents. */
    static String normaliser(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
