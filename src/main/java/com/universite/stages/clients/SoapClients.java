package com.universite.stages.clients;

import com.universite.stages.common.AppConfig;
import com.universite.stages.security.SoapClientSecurityHandler;
import com.universite.stages.soap.commun.Namespaces;
import com.universite.stages.soap.entreprises.EntrepriseService;
import com.universite.stages.soap.etudiants.EtudiantService;
import com.universite.stages.soap.offres.OffreService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabrique de clients SOAP dynamiques. Le contrat est lu depuis le WSDL publié
 * (?wsdl) et les appels transitent réellement par HTTP, avec l'en-tête
 * WS-Security du compte technique de l'orchestrateur.
 */
public final class SoapClients {

    private static final Map<String, Service> SERVICES = new ConcurrentHashMap<>();

    private SoapClients() {
    }

    public static EtudiantService etudiants() {
        return etudiants(AppConfig.get("systeme.username"), AppConfig.get("systeme.password"));
    }

    public static EntrepriseService entreprises() {
        return entreprises(AppConfig.get("systeme.username"), AppConfig.get("systeme.password"));
    }

    public static OffreService offres() {
        return offres(AppConfig.get("systeme.username"), AppConfig.get("systeme.password"));
    }

    public static EtudiantService etudiants(String username, String password) {
        return port("/ws/etudiants", Namespaces.ETUDIANTS, "EtudiantService", "EtudiantPort", EtudiantService.class,
                username, password);
    }

    public static EntrepriseService entreprises(String username, String password) {
        return port("/ws/entreprises", Namespaces.ENTREPRISES, "EntrepriseService", "EntreprisePort",
                EntrepriseService.class, username, password);
    }

    public static OffreService offres(String username, String password) {
        return port("/ws/offres", Namespaces.OFFRES, "OffreService", "OffrePort", OffreService.class, username, password);
    }

    /** Vide le cache (utile après un redémarrage d'un service). */
    public static void reinitialiser() {
        SERVICES.clear();
    }

    private static <T> T port(String chemin, String ns, String serviceName, String portName, Class<T> sei,
                              String username, String password) {
        String url = AppConfig.baseUrl() + chemin;
        Service service = SERVICES.computeIfAbsent(url, u -> {
            try {
                return Service.create(URI.create(u + "?wsdl").toURL(), new QName(ns, serviceName));
            } catch (MalformedURLException e) {
                throw new WebServiceException(e);
            }
        });
        T port = service.getPort(new QName(ns, portName), sei);
        BindingProvider bp = (BindingProvider) port;
        int timeout = AppConfig.getInt("client.timeout-ms", 5000);
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bp.getRequestContext().put("com.sun.xml.ws.connect.timeout", timeout);
        bp.getRequestContext().put("com.sun.xml.ws.request.timeout", timeout);
        @SuppressWarnings("rawtypes")
        List<Handler> chaine = List.of(new SoapClientSecurityHandler(username, password));
        bp.getBinding().setHandlerChain(chaine);
        return port;
    }
}
