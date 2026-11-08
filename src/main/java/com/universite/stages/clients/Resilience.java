package com.universite.stages.clients;

import com.universite.stages.common.AppConfig;
import jakarta.xml.ws.WebServiceException;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Politique de reprise des appels inter-services : N tentatives avec délai
 * exponentiel, uniquement pour les erreurs techniques (indisponibilité).
 * Les erreurs fonctionnelles (4xx, SOAP Fault métier) sont propagées immédiatement.
 */
public final class Resilience {

    private static final Logger LOG = Logger.getLogger(Resilience.class.getName());

    private Resilience() {
    }

    public static <T> T avecReprise(String service, Supplier<T> appel) {
        int tentatives = AppConfig.getInt("client.tentatives", 3);
        long delai = AppConfig.getInt("client.delai-ms", 400);
        RuntimeException derniere = null;
        for (int i = 1; i <= tentatives; i++) {
            try {
                return appel.get();
            } catch (ServiceIndisponibleException e) {
                derniere = e;
            } catch (WebServiceException e) {
                if (estFauteMetier(e)) {
                    throw e;
                }
                derniere = new ServiceIndisponibleException(service, "Service SOAP " + service + " injoignable", e);
            }
            LOG.warning("Appel à " + service + " échoué (tentative " + i + "/" + tentatives + ") : " + derniere.getMessage());
            if (i < tentatives) {
                try {
                    Thread.sleep(delai);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                delai *= 2;
            }
        }
        throw derniere instanceof ServiceIndisponibleException sie ? sie
                : new ServiceIndisponibleException(service, "Service " + service + " indisponible", derniere);
    }

    /** Une SOAPFaultException « Server » est une faute applicative, pas une panne réseau. */
    private static boolean estFauteMetier(WebServiceException e) {
        return e instanceof jakarta.xml.ws.soap.SOAPFaultException;
    }
}
