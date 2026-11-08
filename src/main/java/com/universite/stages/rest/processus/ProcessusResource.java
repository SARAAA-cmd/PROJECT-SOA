package com.universite.stages.rest.processus;

import com.universite.stages.bpmn.ProcessusService;
import com.universite.stages.common.Validation;
import com.universite.stages.rest.commun.ApiException;
import com.universite.stages.rest.commun.Securite;
import com.universite.stages.security.Role;
import com.universite.stages.security.Utilisateur;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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

import java.util.List;
import java.util.Map;

/**
 * Point d'entrée de l'orchestration BPMN.
 * Base : {@code /api/processus}
 */
@Path("/processus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProcessusResource {

    private final ProcessusService service = new ProcessusService();

    @Context
    private SecurityContext sc;

    @Context
    private UriInfo uriInfo;

    /** Soumission d'une candidature : déclenche le processus BPMN. */
    @POST
    @Path("/candidatures")
    @RolesAllowed({Role.ETUDIANT, Role.RESPONSABLE})
    public Response soumettre(Map<String, Object> corps) {
        Utilisateur u = Securite.utilisateur(sc);
        String matricule = u.aRole(Role.ETUDIANT) ? u.refId() : (String) corps.get("matricule");
        Object offre = corps == null ? null : corps.get("offreId");
        Validation v = Validation.create().obligatoire("matricule", matricule).obligatoire("offreId", offre)
                .longueurMax("lettreMotivation", (String) corps.get("lettreMotivation"), 4000);
        if (!v.estValide()) {
            throw ApiException.invalide(v.erreurs());
        }
        Long offreId;
        try {
            offreId = Long.valueOf(offre.toString());
        } catch (NumberFormatException e) {
            throw ApiException.invalide(List.of("offreId doit être numérique"));
        }
        Map<String, Object> r = service.deposer(matricule, offreId, (String) corps.get("lettreMotivation"),
                (String) corps.get("cvUrl"), u.username());
        return Response.created(uriInfo.getBaseUriBuilder().path("processus/instances/{id}")
                .build(r.get("processInstanceId"))).entity(r).build();
    }

    @GET
    @Path("/instances")
    @RolesAllowed(Role.RESPONSABLE)
    public List<Map<String, Object>> instances(@QueryParam("limite") @DefaultValue("50") int limite) {
        return service.instances(Math.min(limite, 200));
    }

    @GET
    @Path("/instances/{id}")
    public Map<String, Object> instance(@PathParam("id") String id) {
        Map<String, Object> r = service.instance(id);
        @SuppressWarnings("unchecked")
        Map<String, Object> vars = (Map<String, Object>) r.get("variables");
        Object ent = vars.get("entrepriseId");
        Securite.exigerProprietaire(sc, (String) vars.get("matricule"),
                ent == null ? null : ((Number) ent).longValue());
        return r;
    }

    @GET
    @Path("/definition")
    @Produces(MediaType.APPLICATION_XML)
    public String definition() {
        return service.definitionXml();
    }

    @GET
    @Path("/taches")
    @RolesAllowed({Role.ENTREPRISE, Role.RESPONSABLE})
    public List<Map<String, Object>> taches() {
        Utilisateur u = Securite.utilisateur(sc);
        if (u.aRole(Role.ENTREPRISE)) {
            return service.taches("evaluer", Long.valueOf(u.refId()));
        }
        return service.taches(null, null);
    }

    @POST
    @Path("/taches/{id}/decision")
    @RolesAllowed({Role.ENTREPRISE, Role.RESPONSABLE})
    public Map<String, Object> decider(@PathParam("id") String id, Map<String, String> corps) {
        verifierAccesTache(id);
        if (corps == null) {
            throw ApiException.invalide(List.of("Corps de requête absent"));
        }
        return service.decider(id, corps, Securite.utilisateur(sc).username());
    }

    /** L'entreprise commence l'examen : la candidature passe EN_COURS_EVALUATION. */
    @POST
    @Path("/taches/{id}/prise-en-charge")
    @RolesAllowed({Role.ENTREPRISE, Role.RESPONSABLE})
    public Map<String, Object> prendreEnCharge(@PathParam("id") String id) {
        verifierAccesTache(id);
        return service.prendreEnCharge(id, Securite.utilisateur(sc).username());
    }

    @POST
    @Path("/taches/{id}/incident")
    @RolesAllowed(Role.RESPONSABLE)
    public Map<String, Object> incident(@PathParam("id") String id, Map<String, String> corps) {
        return service.traiterIncident(id, corps == null ? null : corps.get("action"),
                Securite.utilisateur(sc).username());
    }

    private void verifierAccesTache(String id) {
        Utilisateur u = Securite.utilisateur(sc);
        if (u.aRole(Role.ENTREPRISE)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ctx = (Map<String, Object>) service.tache(id).get("contexte");
            Object ent = ctx.get("entrepriseId");
            if (ent == null || !u.refId().equals(String.valueOf(ent))) {
                throw ApiException.interdit("Cette candidature ne concerne pas votre entreprise");
            }
        }
    }
}
