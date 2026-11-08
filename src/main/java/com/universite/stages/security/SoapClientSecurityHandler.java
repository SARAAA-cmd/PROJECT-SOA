package com.universite.stages.security;

import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import javax.xml.namespace.QName;
import java.util.Set;

/** Intercepteur côté client : ajoute l'en-tête WS-Security UsernameToken. */
public class SoapClientSecurityHandler implements SOAPHandler<SOAPMessageContext> {

    private final String username;
    private final String password;

    public SoapClientSecurityHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean sortant = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (!sortant) {
            return true;
        }
        try {
            SOAPEnvelope env = context.getMessage().getSOAPPart().getEnvelope();
            SOAPHeader header = env.getHeader() != null ? env.getHeader() : env.addHeader();
            SOAPElement security = header.addChildElement("Security", "wsse", Namespaces.WSSE);
            SOAPElement token = security.addChildElement("UsernameToken", "wsse");
            token.addChildElement("Username", "wsse").addTextNode(username);
            token.addChildElement("Password", "wsse").addTextNode(password);
            context.getMessage().saveChanges();
        } catch (SOAPException e) {
            throw new IllegalStateException("Ajout de l'en-tête WS-Security impossible", e);
        }
        return true;
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
        return Set.of(new QName(Namespaces.WSSE, "Security"));
    }
}
