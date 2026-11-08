package com.universite.stages.security;

import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.ws.soap.SOAPFaultException;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Intercepteur côté serveur : exige un en-tête WS-Security UsernameToken
 * (OASIS WSS 1.0) sur chaque requête SOAP, authentifie l'appelant et
 * dépose l'{@link Utilisateur} dans le contexte du message.
 */
public class SoapSecurityHandler implements SOAPHandler<SOAPMessageContext> {

    public static final String UTILISATEUR = "stages.utilisateur";
    private static final QName SECURITY = new QName(Namespaces.WSSE, "Security");
    private static final Logger LOG = Logger.getLogger(SoapSecurityHandler.class.getName());

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean sortant = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (sortant) {
            return true;
        }
        try {
            SOAPHeader header = context.getMessage().getSOAPHeader();
            if (header == null) {
                throw echec("En-tête WS-Security absent");
            }
            SOAPElement security = enfant(header, Namespaces.WSSE, "Security")
                    .orElseThrow(() -> echec("En-tête WS-Security absent"));
            SOAPElement token = enfant(security, Namespaces.WSSE, "UsernameToken")
                    .orElseThrow(() -> echec("UsernameToken absent"));
            String username = enfant(token, Namespaces.WSSE, "Username").map(SOAPElement::getValue).orElse(null);
            String password = enfant(token, Namespaces.WSSE, "Password").map(SOAPElement::getValue).orElse(null);
            Utilisateur u = UtilisateurDao.authentifier(username, password)
                    .orElseThrow(() -> echec("Identifiants invalides"));
            context.put(UTILISATEUR, u);
            context.setScope(UTILISATEUR, MessageContext.Scope.APPLICATION);
            return true;
        } catch (SOAPException e) {
            throw echec("Message SOAP illisible");
        }
    }

    private static Optional<SOAPElement> enfant(SOAPElement parent, String ns, String local) {
        Iterator<?> it = parent.getChildElements(new QName(ns, local));
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof SOAPElement e) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    private static SOAPFaultException echec(String raison) {
        LOG.warning("Accès SOAP refusé : " + raison);
        try {
            SOAPFault fault = SOAPFactory.newInstance().createFault(
                    "Authentification échouée : " + raison,
                    new QName(Namespaces.WSSE, "FailedAuthentication", "wsse"));
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return Set.of(SECURITY);
    }
}
