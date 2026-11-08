package com.universite.stages.rest.stages;

import com.universite.stages.clients.Resilience;
import com.universite.stages.clients.SoapClients;
import com.universite.stages.common.Validation;
import com.universite.stages.rest.commun.ApiException;
import com.universite.stages.rest.commun.Securite;
import com.universite.stages.security.Role;
import com.universite.stages.security.Utilisateur;
import com.universite.stages.soap.commun.ServiceException;
import com.universite.stages.soap.etudiants.Etudiant;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Service 6 — Suivi des stages (API REST).
 * Base : {@code /api/stages}
 */
@Path("/stages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StageResource {

    private static final Logger LOG = Logger.getLogger(StageResource.class.getName());

    static final Map<String, Set<String>> TRANSITIONS = Map.of(
            Stage.PLANIFIE, Set.of(Stage.EN_COURS, Stage.INTERROMPU),
            Stage.EN_COURS, Set.of(Stage.TERMINE, Stage.INTERROMPU),
            Stage.TERMINE, Set.of(),
            Stage.INTERROMPU, Set.of());

    private final StageDao dao = new StageDao();

    @Context
    private SecurityContext sc;

    @Context
    private UriInfo uriInfo;

    /** Enregistrement d'un stage accepté (appelé par l'orchestrateur). */
    @POST
    @RolesAllowed({Role.SYSTEME, Role.RESPONSABLE})
    public Response creer(Stage s) {
        if (s == null) {
            throw ApiException.invalide(List.of("Corps de requête absent"));
        }
        Validation v = Validation.create()
                .obligatoire("candidatureId", s.getCandidatureId())
                .obligatoire("matricule", s.getMatricule())
                .obligatoire("offreId", s.getOffreId())
                .obligatoire("entrepriseId", s.getEntrepriseId())
                .obligatoire("dateDebut", s.getDateDebut())
                .obligatoire("dateFin", s.getDateFin())
                .regle(s.getDateDebut() == null || s.getDateFin() == null || s.getDateFin().isAfter(s.getDateDebut()),
                        "La date de fin doit être postérieure à la date de début");
        if (!v.estValide()) {
            throw ApiException.invalide(v.erreurs());
        }
        dao.parCandidature(s.getCandidatureId()).ifPresent(existant -> {
            throw ApiException.conflit("Un stage existe déjà pour la candidature " + s.getCandidatureId()
                    + " (stage " + existant.getId() + ")");
        });
        s.setId(null);
        s.setStatut(s.getDateDebut().isAfter(LocalDate.now()) ? Stage.PLANIFIE : Stage.EN_COURS);
        s.setDateCreation(LocalDateTime.now());
        dao.inserer(s);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(s.getId())).build())
                .entity(s).build();
    }

    @GET
    public List<Stage> rechercher(@QueryParam("matricule") String matricule,
                                  @QueryParam("entrepriseId") Long entrepriseId,
                                  @QueryParam("statut") String statut,
                                  @QueryParam("candidatureId") Long candidatureId) {
        Utilisateur u = Securite.utilisateur(sc);
        if (u.aRole(Role.ETUDIANT)) {
            matricule = u.refId();
        } else if (u.aRole(Role.ENTREPRISE)) {
            entrepriseId = Long.valueOf(u.refId());
        }
        if (candidatureId != null) {
            String m = matricule;
            Long e = entrepriseId;
            return dao.parCandidature(candidatureId)
                    .filter(s -> (m == null || m.equals(s.getMatricule())) && (e == null || e.equals(s.getEntrepriseId())))
                    .map(List::of).orElse(List.of());
        }
        return dao.rechercher(matricule, entrepriseId, statut);
    }

    @GET
    @Path("/{id}")
    public Stage consulter(@PathParam("id") long id) {
        Stage s = dao.trouver(id).orElseThrow(() -> ApiException.introuvable("Stage " + id + " introuvable"));
        Securite.exigerProprietaire(sc, s.getMatricule(), s.getEntrepriseId());
        return s;
    }

    /** Statut, dates et progression calculée du stage. */
    @GET
    @Path("/{id}/statut")
    public Map<String, Object> statut(@PathParam("id") long id) {
        Stage s = consulter(id);
        long total = ChronoUnit.DAYS.between(s.getDateDebut(), s.getDateFin());
        long ecoules = ChronoUnit.DAYS.between(s.getDateDebut(), LocalDate.now());
        int progression = (int) Math.max(0, Math.min(100, total == 0 ? 100 : ecoules * 100 / total));
        if (Stage.TERMINE.equals(s.getStatut())) {
            progression = 100;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("stageId", s.getId());
        r.put("statut", s.getStatut());
        r.put("dateDebut", s.getDateDebut());
        r.put("dateFin", s.getDateFin());
        r.put("dureeJours", total);
        r.put("joursRestants", Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), s.getDateFin())));
        r.put("progression", progression);
        return r;
    }

    @PATCH
    @Path("/{id}/statut")
    @RolesAllowed({Role.RESPONSABLE, Role.SYSTEME, Role.ENTREPRISE})
    public Stage changerStatut(@PathParam("id") long id, Map<String, String> corps) {
        String nouveau = corps == null ? null : corps.get("statut");
        if (nouveau == null || !TRANSITIONS.containsKey(nouveau)) {
            throw ApiException.invalide(List.of("Statut attendu parmi " + TRANSITIONS.keySet()));
        }
        Stage s = consulter(id);
        if (!TRANSITIONS.get(s.getStatut()).contains(nouveau)) {
            throw new ApiException(409, "TRANSITION_INTERDITE",
                    "Transition " + s.getStatut() + " → " + nouveau + " non autorisée", List.of());
        }
        dao.majStatut(id, nouveau);
        s.setStatut(nouveau);
        if (Stage.TERMINE.equals(nouveau) || Stage.INTERROMPU.equals(nouveau)) {
            libererEtudiant(s.getMatricule());
        }
        return s;
    }

    @POST
    @Path("/{id}/comptes-rendus")
    public Response ajouterCompteRendu(@PathParam("id") long id, CompteRendu cr) {
        Stage s = consulter(id);
        if (!Set.of(Stage.PLANIFIE, Stage.EN_COURS).contains(s.getStatut())) {
            throw ApiException.regle("Impossible d'ajouter un compte rendu à un stage " + s.getStatut());
        }
        Validation v = Validation.create()
                .obligatoire("periode", cr == null ? null : cr.periode())
                .obligatoire("contenu", cr == null ? null : cr.contenu())
                .longueurMax("contenu", cr == null ? null : cr.contenu(), 4000);
        if (!v.estValide()) {
            throw ApiException.invalide(v.erreurs());
        }
        CompteRendu cree = dao.ajouterCompteRendu(new CompteRendu(null, id, cr.periode(), cr.contenu(),
                Securite.utilisateur(sc).username(), LocalDateTime.now()));
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(cree.id())).build())
                .entity(cree).build();
    }

    @GET
    @Path("/{id}/comptes-rendus")
    public List<CompteRendu> comptesRendus(@PathParam("id") long id) {
        consulter(id);
        return dao.comptesRendus(id);
    }

    @POST
    @Path("/{id}/evaluations")
    @RolesAllowed({Role.RESPONSABLE, Role.ENTREPRISE})
    public Response evaluer(@PathParam("id") long id, Evaluation ev) {
        Stage s = consulter(id);
        Validation v = Validation.create()
                .obligatoire("type", ev == null ? null : ev.type())
                .dansListe("type", ev == null ? null : ev.type(), List.of("MI_PARCOURS", "FINALE"))
                .obligatoire("evaluateur", ev == null ? null : ev.evaluateur())
                .obligatoire("note", ev == null ? null : ev.note())
                .entre("note", ev == null ? null : ev.note(), 0, 20);
        if (!v.estValide()) {
            throw ApiException.invalide(v.erreurs());
        }
        boolean finale = "FINALE".equals(ev.type());
        if (finale && dao.evaluations(id).stream().anyMatch(e -> "FINALE".equals(e.type()))) {
            throw ApiException.conflit("L'évaluation finale de ce stage a déjà été enregistrée");
        }
        if (Stage.INTERROMPU.equals(s.getStatut()) || Stage.TERMINE.equals(s.getStatut())) {
            throw ApiException.regle("Le stage est " + s.getStatut() + " : aucune évaluation possible");
        }
        Evaluation cree = dao.ajouterEvaluation(new Evaluation(null, id, ev.type(), ev.evaluateur(),
                ev.note().setScale(2, RoundingMode.HALF_UP), ev.appreciation(), LocalDateTime.now()));
        // l'évaluation finale clôture le stage
        if (finale) {
            dao.majStatut(id, Stage.TERMINE);
            libererEtudiant(s.getMatricule());
        }
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(cree.id())).build())
                .entity(cree).build();
    }

    @GET
    @Path("/{id}/evaluations")
    public List<Evaluation> evaluations(@PathParam("id") long id) {
        consulter(id);
        return dao.evaluations(id);
    }

    /** Historique des stages réalisés par un étudiant, avec moyenne des évaluations. */
    @GET
    @Path("/etudiant/{matricule}/historique")
    public List<Map<String, Object>> historique(@PathParam("matricule") String matricule) {
        Securite.exigerEtudiant(sc, matricule);
        return dao.rechercher(matricule, null, null).stream().map(s -> {
            List<Evaluation> evals = dao.evaluations(s.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stage", s);
            m.put("nombreComptesRendus", dao.comptesRendus(s.getId()).size());
            m.put("evaluations", evals);
            if (!evals.isEmpty()) {
                BigDecimal somme = evals.stream().map(Evaluation::note).reduce(BigDecimal.ZERO, BigDecimal::add);
                m.put("moyenne", somme.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP));
            }
            return m;
        }).toList();
    }

    /** Fin de stage : l'étudiant redevient ACTIF (appel SOAP au service Étudiants). */
    private static void libererEtudiant(String matricule) {
        try {
            Resilience.avecReprise("etudiants", () -> {
                try {
                    return SoapClients.etudiants().changerStatutEtudiant(matricule, Etudiant.ACTIF);
                } catch (ServiceException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            });
        } catch (RuntimeException e) {
            LOG.warning("Mise à jour du statut de l'étudiant " + matricule + " impossible : " + e.getMessage());
        }
    }
}
