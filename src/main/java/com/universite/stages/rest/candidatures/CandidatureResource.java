package com.universite.stages.rest.candidatures;

import com.universite.stages.common.AppConfig;
import com.universite.stages.common.Validation;
import com.universite.stages.rest.commun.ApiException;
import com.universite.stages.rest.commun.Securite;
import com.universite.stages.security.Role;
import com.universite.stages.security.Utilisateur;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service 4 — Gestion des candidatures (API REST).
 * Base : {@code /api/candidatures}
 */
@Path("/candidatures")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CandidatureResource {

    /** Automate des statuts : transitions autorisées depuis chaque statut. */
    static final Map<String, Set<String>> TRANSITIONS = Map.of(
            Candidature.DEPOSEE, Set.of(Candidature.EN_COURS_EVALUATION, Candidature.REFUSEE, Candidature.ANNULEE),
            Candidature.EN_COURS_EVALUATION, Set.of(Candidature.ACCEPTEE, Candidature.REFUSEE),
            Candidature.ACCEPTEE, Set.of(),
            Candidature.REFUSEE, Set.of(),
            Candidature.ANNULEE, Set.of());

    private final CandidatureDao dao = new CandidatureDao();

    @Context
    private SecurityContext sc;

    @Context
    private UriInfo uriInfo;

    /** Dépôt (enregistrement) d'une candidature — appelé par l'orchestrateur BPMN. */
    @POST
    @RolesAllowed({Role.SYSTEME, Role.RESPONSABLE})
    public Response deposer(Candidature c) {
        if (c == null) {
            throw ApiException.invalide(List.of("Corps de requête absent"));
        }
        Validation v = Validation.create()
                .obligatoire("matricule", c.getMatricule())
                .obligatoire("offreId", c.getOffreId())
                .longueurMax("lettreMotivation", c.getLettreMotivation(), 4000)
                .longueurMax("cvUrl", c.getCvUrl(), 300);
        if (!v.estValide()) {
            throw ApiException.invalide(v.erreurs());
        }
        int max = AppConfig.getInt("regles.max-candidatures-actives", 5);
        if (dao.nombreActives(c.getMatricule()) >= max) {
            throw ApiException.regle("L'étudiant " + c.getMatricule() + " a déjà " + max
                    + " candidatures en cours (maximum autorisé)");
        }
        dao.doublon(c.getMatricule(), c.getOffreId()).ifPresent(d -> {
            throw ApiException.conflit("L'étudiant " + c.getMatricule() + " a déjà candidaté à cette offre (candidature "
                    + d.getId() + ")");
        });
        LocalDateTime now = LocalDateTime.now();
        c.setId(null);
        c.setStatut(Candidature.DEPOSEE);
        c.setDateDepot(now);
        c.setDateMiseAJour(now);
        dao.inserer(c);
        dao.historiser(c.getId(), null, Candidature.DEPOSEE, "Candidature déposée", auteur());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(c.getId())).build())
                .entity(c).build();
    }

    @GET
    public List<Candidature> rechercher(@QueryParam("matricule") String matricule,
                                        @QueryParam("offreId") Long offreId,
                                        @QueryParam("entrepriseId") Long entrepriseId,
                                        @QueryParam("statut") String statut) {
        Utilisateur u = Securite.utilisateur(sc);
        // restriction automatique du périmètre selon l'acteur connecté
        if (u.aRole(Role.ETUDIANT)) {
            matricule = u.refId();
        } else if (u.aRole(Role.ENTREPRISE)) {
            entrepriseId = Long.valueOf(u.refId());
        }
        return dao.rechercher(matricule, offreId, entrepriseId, statut);
    }

    @GET
    @Path("/etudiant/{matricule}")
    public List<Candidature> parEtudiant(@PathParam("matricule") String matricule) {
        Securite.exigerEtudiant(sc, matricule);
        return dao.rechercher(matricule, null, null, null);
    }

    @GET
    @Path("/{id}")
    public Candidature consulter(@PathParam("id") long id) {
        Candidature c = charger(id);
        Securite.exigerProprietaire(sc, c.getMatricule(), c.getEntrepriseId());
        return c;
    }

    @GET
    @Path("/{id}/historique")
    public List<ChangementStatut> historique(@PathParam("id") long id) {
        consulter(id);
        return dao.historique(id);
    }

    /** Complément d'information : l'étudiant met à jour sa lettre ou son CV. */
    @PATCH
    @Path("/{id}")
    @RolesAllowed({Role.ETUDIANT, Role.RESPONSABLE})
    public Candidature completer(@PathParam("id") long id, Candidature maj) {
        Candidature c = charger(id);
        Securite.exigerProprietaire(sc, c.getMatricule(), c.getEntrepriseId());
        if (!Set.of(Candidature.DEPOSEE, Candidature.EN_COURS_EVALUATION).contains(c.getStatut())) {
            throw ApiException.regle("Une candidature " + c.getStatut() + " ne peut plus être modifiée");
        }
        if (maj.getLettreMotivation() != null) {
            c.setLettreMotivation(maj.getLettreMotivation());
        }
        if (maj.getCvUrl() != null) {
            c.setCvUrl(maj.getCvUrl());
        }
        c.setDateMiseAJour(LocalDateTime.now());
        dao.mettreAJour(c);
        dao.historiser(id, c.getStatut(), c.getStatut(), "Dossier complété par l'étudiant", auteur());
        return c;
    }

    @PATCH
    @Path("/{id}/statut")
    @RolesAllowed({Role.SYSTEME, Role.RESPONSABLE, Role.ENTREPRISE})
    public Candidature changerStatut(@PathParam("id") long id, StatutRequest req) {
        if (req == null || req.statut() == null) {
            throw ApiException.invalide(List.of("Le champ 'statut' est obligatoire"));
        }
        if (!TRANSITIONS.containsKey(req.statut())) {
            throw ApiException.invalide(List.of("Statut inconnu : " + req.statut() + " (attendu : " + TRANSITIONS.keySet() + ")"));
        }
        Candidature c = charger(id);
        Securite.exigerProprietaire(sc, c.getMatricule(), c.getEntrepriseId());
        if (req.statut().equals(c.getStatut())) {
            return c; // idempotent
        }
        // une décision sur une candidature orchestrée doit passer par la tâche BPMN de l'entreprise
        boolean decision = Set.of(Candidature.ACCEPTEE, Candidature.REFUSEE).contains(req.statut());
        if (decision && c.getProcessInstanceId() != null && !Securite.utilisateur(sc).aRole(Role.SYSTEME)) {
            throw new ApiException(409, "DECISION_VIA_PROCESSUS",
                    "La décision doit être rendue via la tâche « Évaluer la candidature » du processus "
                            + c.getProcessInstanceId() + " (POST /api/processus/taches/{id}/decision)", List.of());
        }
        if (!TRANSITIONS.get(c.getStatut()).contains(req.statut())) {
            throw new ApiException(409, "TRANSITION_INTERDITE",
                    "Transition " + c.getStatut() + " → " + req.statut() + " non autorisée", List.of());
        }
        String ancien = c.getStatut();
        c.setStatut(req.statut());
        c.setMotif(req.motif());
        c.setDateMiseAJour(LocalDateTime.now());
        dao.mettreAJour(c);
        dao.historiser(id, ancien, req.statut(), req.motif(), auteur());
        return c;
    }

    /** Annulation par l'étudiant tant que l'entreprise n'a pas commencé l'examen (statut DEPOSEE). */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({Role.ETUDIANT, Role.RESPONSABLE, Role.SYSTEME})
    public Candidature annuler(@PathParam("id") long id) {
        Candidature c = charger(id);
        Securite.exigerProprietaire(sc, c.getMatricule(), c.getEntrepriseId());
        if (!TRANSITIONS.get(c.getStatut()).contains(Candidature.ANNULEE)) {
            throw ApiException.regle("Annulation impossible : la candidature est " + c.getStatut()
                    + " (seule une candidature DEPOSEE peut être annulée)");
        }
        String ancien = c.getStatut();
        c.setStatut(Candidature.ANNULEE);
        c.setMotif("Annulée à la demande de " + auteur());
        c.setDateMiseAJour(LocalDateTime.now());
        dao.mettreAJour(c);
        dao.historiser(id, ancien, Candidature.ANNULEE, c.getMotif(), auteur());
        CandidatureEvenements.publierAnnulation(c);
        return c;
    }

    private Candidature charger(long id) {
        return dao.trouver(id).orElseThrow(() -> ApiException.introuvable("Candidature " + id + " introuvable"));
    }

    private String auteur() {
        return Securite.utilisateur(sc).username();
    }
}
