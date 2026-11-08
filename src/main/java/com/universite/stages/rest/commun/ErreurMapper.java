package com.universite.stages.rest.commun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.universite.stages.clients.ServiceIndisponibleException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Traduit toute exception en réponse JSON avec le code HTTP adapté. */
@Provider
public class ErreurMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErreurMapper.class.getName());

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(Throwable ex) {
        Erreur e;
        if (ex instanceof ApiException api) {
            e = new Erreur(api.getStatus(), api.getCode(), api.getMessage(), api.getDetails());
        } else if (ex instanceof JsonProcessingException jpe) {
            e = new Erreur(400, "JSON_INVALIDE", "Corps JSON invalide : " + jpe.getOriginalMessage(), null);
        } else if (ex instanceof ServiceIndisponibleException sie) {
            e = new Erreur(503, "SERVICE_INDISPONIBLE", sie.getMessage(), null);
        } else if (ex instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String code = switch (status) {
                case 401 -> "NON_AUTHENTIFIE";
                case 403 -> "ACCES_REFUSE";
                case 404 -> "RESSOURCE_INTROUVABLE";
                case 405 -> "METHODE_NON_AUTORISEE";
                case 415 -> "TYPE_MEDIA_NON_SUPPORTE";
                default -> "ERREUR_HTTP";
            };
            String message = status == 403 ? "Accès refusé pour votre rôle" : wae.getMessage();
            e = new Erreur(status, code, message, null);
        } else {
            LOG.log(Level.SEVERE, "Erreur interne", ex);
            e = new Erreur(500, "ERREUR_INTERNE", "Erreur interne du serveur", List.of(ex.getClass().getSimpleName()));
        }
        Response.ResponseBuilder rb = Response.status(e.status()).type(MediaType.APPLICATION_JSON_TYPE).entity(e);
        // pas de défi Basic pour l'interface web : évite la fenêtre d'authentification native du navigateur
        boolean appelAjax = headers != null && "fetch".equals(headers.getHeaderString("X-Requested-With"));
        if (e.status() == 401 && !appelAjax) {
            rb.header("WWW-Authenticate", "Basic realm=\"stages\"");
        }
        return rb.build();
    }
}
