package com.universite.stages.rest.commun;

import com.universite.stages.common.SimulateurPannes;
import com.universite.stages.security.Role;
import com.universite.stages.security.Utilisateur;
import com.universite.stages.security.UtilisateurDao;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Points d'accès transverses : santé de la plateforme et profil connecté. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SystemeResource {

    @GET
    @Path("sante")
    @PermitAll
    public Map<String, Object> sante() {
        return Map.of("statut", "OK", "horodatage", OffsetDateTime.now().toString());
    }

    @GET
    @Path("moi")
    @PermitAll
    public Map<String, Object> moi(@Context SecurityContext sc) {
        Utilisateur u = Securite.utilisateur(sc);
        return u.refId() == null
                ? Map.of("username", u.username(), "role", u.role())
                : Map.of("username", u.username(), "role", u.role(), "refId", u.refId());
    }

    /** Génère une nouvelle clé d'API pour l'utilisateur connecté ; l'ancienne est révoquée. */
    @POST
    @Path("moi/cle-api")
    @RolesAllowed(Role.RESPONSABLE) // génération de clé réservée au responsable des stages
    public Map<String, Object> nouvelleCle(@Context SecurityContext sc) {
        Utilisateur u = Securite.utilisateur(sc);
        return Map.of("username", u.username(), "cleApi", UtilisateurDao.genererCle(u.username()),
                "enTete", "X-API-Key", "avertissement", "Conservez cette clé : elle ne sera plus affichée.");
    }

    @GET
    @Path("admin/pannes")
    @RolesAllowed(Role.RESPONSABLE)
    public Map<String, Object> pannes() {
        return Map.of("servicesEnPanne", SimulateurPannes.enPanne(), "services", SimulateurPannes.SERVICES);
    }

    /** Simule (ou rétablit) l'indisponibilité d'un service : {"service":"candidatures","panne":true}. */
    @POST
    @Path("admin/pannes")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed(Role.RESPONSABLE)
    public Map<String, Object> simulerPanne(Map<String, Object> corps) {
        Object service = corps == null ? null : corps.get("service");
        if (service == null || !SimulateurPannes.SERVICES.contains(service.toString())) {
            throw ApiException.invalide(List.of("service doit valoir l'un de " + SimulateurPannes.SERVICES));
        }
        SimulateurPannes.definir(service.toString(), Boolean.TRUE.equals(corps.get("panne")));
        return pannes();
    }
}
