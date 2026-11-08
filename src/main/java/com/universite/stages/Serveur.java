package com.universite.stages;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.universite.stages.bpmn.MoteurBpmn;
import com.universite.stages.clients.SoapClients;
import com.universite.stages.common.AppConfig;
import com.universite.stages.common.Database;
import com.universite.stages.common.SimulateurPannes;
import com.universite.stages.rest.candidatures.CandidatureResource;
import com.universite.stages.rest.commun.AuthentificationFilter;
import com.universite.stages.rest.commun.ErreurMapper;
import com.universite.stages.rest.commun.JacksonConfig;
import com.universite.stages.rest.commun.SystemeResource;
import com.universite.stages.rest.notifications.NotificationResource;
import com.universite.stages.rest.notifications.NotificationService;
import com.universite.stages.rest.processus.ProcessusResource;
import com.universite.stages.rest.stages.StageResource;
import com.universite.stages.security.SoapSecurityHandler;
import com.universite.stages.soap.entreprises.EntrepriseServiceImpl;
import com.universite.stages.soap.etudiants.EtudiantServiceImpl;
import com.universite.stages.soap.offres.OffreServiceImpl;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.handler.Handler;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Hôte des services : un serveur HTTP unique expose
 * <ul>
 *   <li>les 3 services SOAP JAX-WS sous {@code /ws/*},</li>
 *   <li>les 3 services REST JAX-RS (+ orchestration) sous {@code /api/*},</li>
 *   <li>l'interface web sous {@code /}.</li>
 * </ul>
 * Chaque service reste autonome (contrat, données, sécurité) et n'interagit
 * avec les autres que par appels réseau SOAP/REST.
 */
public class Serveur {

    private static final Logger LOG = Logger.getLogger(Serveur.class.getName());

    private HttpServer http;
    private final List<Endpoint> endpoints = new ArrayList<>();
    private ScheduledExecutorService planificateur;

    public void demarrer() throws IOException {
        Database.init();
        DonneesDemo.charger();

        int port = AppConfig.getInt("server.port", 8090);
        http = HttpServer.create(new InetSocketAddress(port), 0);
        // un fil virtuel par requête : les services peuvent s'appeler entre eux sans blocage
        http.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        publierSoap("/ws/etudiants", new EtudiantServiceImpl());
        publierSoap("/ws/entreprises", new EntrepriseServiceImpl());
        publierSoap("/ws/offres", new OffreServiceImpl());

        ResourceConfig rc = new ResourceConfig()
                .register(CandidatureResource.class)
                .register(NotificationResource.class)
                .register(StageResource.class)
                .register(ProcessusResource.class)
                .register(SystemeResource.class)
                .register(AuthentificationFilter.class)
                .register(RolesAllowedDynamicFeature.class)
                .register(JacksonFeature.withoutExceptionMappers())
                .register(JacksonConfig.class)
                .register(ErreurMapper.class);
        HttpHandler rest = ContainerFactory.createContainer(HttpHandler.class, rc);
        http.createContext("/api", rest).getFilters().add(SimulateurPannes.filtre());
        http.createContext("/", new FichiersStatiques("/public"));

        // le processus est déployé avant l'ouverture du port : aucune requête ne peut le précéder
        MoteurBpmn.demarrer();
        SoapClients.reinitialiser();
        http.start();
        planifierRappels();
        LOG.info("Plateforme démarrée sur " + AppConfig.baseUrl());
    }

    private void publierSoap(String chemin, Object implementation) {
        Endpoint ep = Endpoint.create(implementation);
        @SuppressWarnings("rawtypes")
        List<Handler> chaine = List.of(new SoapSecurityHandler());
        ep.getBinding().setHandlerChain(chaine);
        HttpContext ctx = http.createContext(chemin);
        ctx.getFilters().add(SimulateurPannes.filtre());
        ep.publish(ctx);
        endpoints.add(ep);
    }

    /** Rappels automatiques des dates limites (service Notifications). */
    private void planifierRappels() {
        int periode = AppConfig.getInt("rappels.periode-heures", 24);
        if (periode <= 0) {
            return;
        }
        planificateur = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        planificateur.scheduleAtFixedRate(() -> {
            try {
                var r = new NotificationService().envoyerRappels(AppConfig.getInt("rappels.jours-avant", 3));
                LOG.info("Campagne de rappels : " + r);
            } catch (RuntimeException e) {
                LOG.warning("Campagne de rappels échouée : " + e.getMessage());
            }
        }, 1, periode * 60L, TimeUnit.MINUTES);
    }

    public void arreter() {
        if (planificateur != null) {
            planificateur.shutdownNow();
        }
        MoteurBpmn.arreter();
        endpoints.forEach(Endpoint::stop);
        endpoints.clear();
        if (http != null) {
            http.stop(0);
        }
    }
}
