package com.universite.stages.rest.commun;

import com.universite.stages.security.Utilisateur;
import com.universite.stages.security.UtilisateurDao;
import jakarta.annotation.Priority;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;

/**
 * Authentification de toutes les requêtes REST (hors /sante) :
 * <ul>
 *   <li>clé d'API dans l'en-tête {@code X-API-Key} (applications, scripts, orchestrateur) ;</li>
 *   <li>ou HTTP Basic (interface web, après connexion de l'utilisateur).</li>
 * </ul>
 * Le contrôle des rôles est ensuite assuré par @RolesAllowed.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthentificationFilter implements ContainerRequestFilter {

    public static final String EN_TETE_CLE = "X-API-Key";

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (ctx.getUriInfo().getPath().startsWith("sante")) {
            return;
        }
        String cle = ctx.getHeaderString(EN_TETE_CLE);
        Utilisateur u;
        if (cle != null) {
            u = UtilisateurDao.authentifierParCle(cle).orElseThrow(() ->
                    new ApiException(401, "CLE_API_INVALIDE", "Clé d'API invalide ou révoquée", java.util.List.of()));
        } else {
            u = lire(ctx.getHeaderString(HttpHeaders.AUTHORIZATION))
                    .orElseThrow(() -> new NotAuthorizedException("Authentification requise", "Basic realm=\"stages\""));
        }
        boolean https = "https".equals(ctx.getUriInfo().getRequestUri().getScheme());
        ctx.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return u;
            }

            @Override
            public boolean isUserInRole(String role) {
                return u.aRole(role);
            }

            @Override
            public boolean isSecure() {
                return https;
            }

            @Override
            public String getAuthenticationScheme() {
                return cle != null ? "API_KEY" : BASIC_AUTH;
            }
        });
    }

    private static Optional<Utilisateur> lire(String header) {
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }
        try {
            String decode = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int sep = decode.indexOf(':');
            if (sep < 0) {
                return Optional.empty();
            }
            return UtilisateurDao.authentifier(decode.substring(0, sep), decode.substring(sep + 1));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
