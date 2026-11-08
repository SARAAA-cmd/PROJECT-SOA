package com.universite.stages.common;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outil de démonstration : rend un service indisponible (HTTP 503) afin de
 * montrer la politique de reprise et la gestion des erreurs du processus BPMN.
 */
public final class SimulateurPannes {

    public static final List<String> SERVICES =
            List.of("etudiants", "entreprises", "offres", "candidatures", "notifications", "stages");
    private static final Set<String> EN_PANNE = ConcurrentHashMap.newKeySet();

    private SimulateurPannes() {
    }

    public static void definir(String service, boolean panne) {
        if (!SERVICES.contains(service)) {
            throw new IllegalArgumentException("Service inconnu : " + service);
        }
        if (panne) {
            EN_PANNE.add(service);
        } else {
            EN_PANNE.remove(service);
        }
    }

    public static Set<String> enPanne() {
        return Set.copyOf(EN_PANNE);
    }

    public static void toutRetablir() {
        EN_PANNE.clear();
    }

    /** Filtre HTTP : /ws/{service} et /api/{service} répondent 503 si le service est en panne. */
    public static Filter filtre() {
        return new Filter() {
            @Override
            public void doFilter(HttpExchange ex, Chain chain) throws IOException {
                String[] seg = ex.getRequestURI().getPath().split("/");
                boolean wsdl = ex.getRequestURI().getQuery() != null && ex.getRequestURI().getQuery().contains("wsdl");
                if (seg.length > 2 && EN_PANNE.contains(seg[2]) && !wsdl) {
                    byte[] corps = ("{\"status\":503,\"erreur\":\"SERVICE_INDISPONIBLE\",\"message\":\"Service "
                            + seg[2] + " indisponible (panne simulée)\"}").getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().add("Content-Type", "application/json");
                    ex.getResponseHeaders().add("Retry-After", "30");
                    ex.sendResponseHeaders(503, corps.length);
                    ex.getResponseBody().write(corps);
                    ex.close();
                    return;
                }
                chain.doFilter(ex);
            }

            @Override
            public String description() {
                return "Simulateur de pannes";
            }
        };
    }
}
