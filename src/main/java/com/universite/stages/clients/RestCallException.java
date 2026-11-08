package com.universite.stages.clients;

import com.fasterxml.jackson.databind.JsonNode;

/** Réponse 4xx d'un service REST : erreur fonctionnelle, à ne pas rejouer. */
public class RestCallException extends RuntimeException {

    private final int status;
    private final JsonNode corps;

    public RestCallException(int status, JsonNode corps) {
        super("HTTP " + status + (corps != null && corps.has("message") ? " : " + corps.get("message").asText() : ""));
        this.status = status;
        this.corps = corps;
    }

    public int getStatus() {
        return status;
    }

    public JsonNode getCorps() {
        return corps;
    }
}
