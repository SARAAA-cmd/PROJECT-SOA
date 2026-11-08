package com.universite.stages.security;

import com.universite.stages.soap.commun.ServiceException;
import jakarta.xml.ws.WebServiceContext;

/** Contrôle d'accès par rôle dans les implémentations SOAP. */
public final class SoapSecurity {

    private SoapSecurity() {
    }

    public static Utilisateur utilisateur(WebServiceContext ctx) throws ServiceException {
        Object u = ctx == null ? null : ctx.getMessageContext().get(SoapSecurityHandler.UTILISATEUR);
        if (u instanceof Utilisateur utilisateur) {
            return utilisateur;
        }
        throw ServiceException.of(ServiceException.ACCES_REFUSE, "Appelant non authentifié");
    }

    public static Utilisateur exiger(WebServiceContext ctx, String... roles) throws ServiceException {
        Utilisateur u = utilisateur(ctx);
        if (!u.aRole(roles)) {
            throw ServiceException.of(ServiceException.ACCES_REFUSE,
                    "Opération réservée aux rôles " + String.join(", ", roles));
        }
        return u;
    }
}
