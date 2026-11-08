package com.universite.stages.rest.notifications;

import com.universite.stages.rest.commun.ApiException;
import com.universite.stages.rest.commun.Securite;
import com.universite.stages.security.Role;
import com.universite.stages.security.Utilisateur;
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

import java.util.List;
import java.util.Map;

/**
 * Service 5 — Notifications (API REST).
 * Base : {@code /api/notifications}
 */
@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private final NotificationService service = new NotificationService();
    private final NotificationDao dao = new NotificationDao();

    @Context
    private SecurityContext sc;

    @Context
    private UriInfo uriInfo;

    @POST
    @RolesAllowed({Role.SYSTEME, Role.RESPONSABLE})
    public Response envoyer(Notification n) {
        if (n == null) {
            throw ApiException.invalide(List.of("Corps de requête absent"));
        }
        Notification envoyee = service.envoyer(n);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(envoyee.getId())).build())
                .entity(envoyee).build();
    }

    @GET
    public List<Notification> rechercher(@QueryParam("destinataireType") String destinataireType,
                                         @QueryParam("destinataireId") String destinataireId,
                                         @QueryParam("candidatureId") Long candidatureId,
                                         @QueryParam("type") String type) {
        Utilisateur u = Securite.utilisateur(sc);
        if (u.aRole(Role.ETUDIANT)) {
            destinataireType = "ETUDIANT";
            destinataireId = u.refId();
        } else if (u.aRole(Role.ENTREPRISE)) {
            destinataireType = "ENTREPRISE";
            destinataireId = u.refId();
        }
        return dao.rechercher(destinataireType, destinataireId, candidatureId, type);
    }

    @GET
    @Path("/{id}")
    public Notification consulter(@PathParam("id") long id) {
        Notification n = dao.trouver(id).orElseThrow(() -> ApiException.introuvable("Notification " + id + " introuvable"));
        verifierDestinataire(n);
        return n;
    }

    @PATCH
    @Path("/{id}/lue")
    public Notification marquerLue(@PathParam("id") long id) {
        Notification n = consulter(id);
        dao.marquerLue(id);
        n.setLue(true);
        return n;
    }

    /** Déclenche la campagne de rappels des dates limites. */
    @POST
    @Path("/rappels")
    @RolesAllowed({Role.SYSTEME, Role.RESPONSABLE})
    public Map<String, Object> rappels(Map<String, Integer> corps) {
        int jours = corps == null || corps.get("joursAvant") == null ? 3 : corps.get("joursAvant");
        if (jours < 0 || jours > 60) {
            throw ApiException.invalide(List.of("joursAvant doit être compris entre 0 et 60"));
        }
        return service.envoyerRappels(jours);
    }

    private void verifierDestinataire(Notification n) {
        Utilisateur u = Securite.utilisateur(sc);
        boolean autorise = u.estInterne()
                || (u.aRole(Role.ETUDIANT) && "ETUDIANT".equals(n.getDestinataireType()) && u.refId().equals(n.getDestinataireId()))
                || (u.aRole(Role.ENTREPRISE) && "ENTREPRISE".equals(n.getDestinataireType()) && u.refId().equals(n.getDestinataireId()));
        if (!autorise) {
            throw ApiException.interdit("Cette notification ne vous est pas destinée");
        }
    }
}
