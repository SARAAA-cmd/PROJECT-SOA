package com.universite.stages;

import com.fasterxml.jackson.databind.JsonNode;
import com.universite.stages.common.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Petit client HTTP pour les tests d'intégration REST. */
final class Http {

    record Reponse(int status, JsonNode json, String brut) {
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    static String base;

    private Http() {
    }

    static Reponse appel(String methode, String chemin, String user, String pass, Object corps) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + chemin));
            if (user != null) {
                b.header("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8)));
            }
            if (corps != null) {
                String s = corps instanceof String str ? str : Json.MAPPER.writeValueAsString(corps);
                b.header("Content-Type", "application/json").method(methode, HttpRequest.BodyPublishers.ofString(s));
            } else {
                b.method(methode, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = null;
            try {
                json = r.body().isBlank() ? null : Json.MAPPER.readTree(r.body());
            } catch (Exception ignored) {
                // corps non JSON (WSDL, XML)
            }
            return new Reponse(r.statusCode(), json, r.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Reponse get(String chemin, String user, String pass) {
        return appel("GET", chemin, user, pass, null);
    }

    static Reponse post(String chemin, String user, String pass, Object corps) {
        return appel("POST", chemin, user, pass, corps);
    }

    static Reponse postSoap(String chemin, String enveloppe) {
        try {
            HttpRequest r = HttpRequest.newBuilder(URI.create(base + chemin))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(enveloppe)).build();
            HttpResponse<String> resp = CLIENT.send(r, HttpResponse.BodyHandlers.ofString());
            return new Reponse(resp.statusCode(), null, resp.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
