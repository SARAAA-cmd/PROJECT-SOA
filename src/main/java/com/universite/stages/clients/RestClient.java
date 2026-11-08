package com.universite.stages.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.universite.stages.common.AppConfig;
import com.universite.stages.common.Json;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Client HTTP/JSON des services REST, authentifié par la clé d'API du compte technique. */
public final class RestClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(AppConfig.getInt("client.timeout-ms", 5000)))
            .build();

    private RestClient() {
    }

    public static JsonNode get(String chemin) {
        return envoyer("GET", chemin, null);
    }

    public static JsonNode post(String chemin, Object corps) {
        return envoyer("POST", chemin, corps);
    }

    public static JsonNode patch(String chemin, Object corps) {
        return envoyer("PATCH", chemin, corps);
    }

    public static JsonNode delete(String chemin) {
        return envoyer("DELETE", chemin, null);
    }

    private static JsonNode envoyer(String methode, String chemin, Object corps) {
        String[] segments = chemin.split("\\?")[0].split("/");
        String service = segments.length > 2 ? segments[2] : chemin;
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(AppConfig.baseUrl() + chemin))
                    .timeout(Duration.ofMillis(AppConfig.getInt("client.timeout-ms", 5000)))
                    .header("Accept", "application/json")
                    .header("X-API-Key", AppConfig.get("systeme.api-key"));
            if (corps != null) {
                b.header("Content-Type", "application/json")
                        .method(methode, HttpRequest.BodyPublishers.ofString(Json.MAPPER.writeValueAsString(corps)));
            } else {
                b.method(methode, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = r.body() == null || r.body().isBlank() ? null : lire(r.body());
            if (r.statusCode() >= 500) {
                throw new ServiceIndisponibleException(service,
                        "Service REST " + service + " en erreur (HTTP " + r.statusCode() + ")", null);
            }
            if (r.statusCode() >= 400) {
                throw new RestCallException(r.statusCode(), json);
            }
            return json;
        } catch (IOException e) {
            throw new ServiceIndisponibleException(service, "Service REST " + service + " injoignable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceIndisponibleException(service, "Appel interrompu", e);
        }
    }

    private static JsonNode lire(String body) {
        try {
            return Json.MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
