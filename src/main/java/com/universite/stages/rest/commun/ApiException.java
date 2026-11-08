package com.universite.stages.rest.commun;

import jakarta.ws.rs.core.Response;

import java.util.List;

/** Erreur fonctionnelle REST, convertie en réponse JSON par {@link ErreurMapper}. */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;
    private final List<String> details;

    public ApiException(Response.Status status, String code, String message) {
        this(status.getStatusCode(), code, message, List.of());
    }

    public ApiException(int status, String code, String message, List<String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public List<String> getDetails() { return details; }

    public static ApiException introuvable(String message) {
        return new ApiException(Response.Status.NOT_FOUND, "RESSOURCE_INTROUVABLE", message);
    }

    public static ApiException invalide(List<String> erreurs) {
        return new ApiException(400, "DONNEES_INVALIDES", "Données invalides", erreurs);
    }

    public static ApiException conflit(String message) {
        return new ApiException(Response.Status.CONFLICT, "CONFLIT", message);
    }

    public static ApiException regle(String message) {
        return new ApiException(422, "REGLE_METIER", message, List.of());
    }

    public static ApiException interdit(String message) {
        return new ApiException(Response.Status.FORBIDDEN, "ACCES_REFUSE", message);
    }

    public static ApiException indisponible(String message) {
        return new ApiException(Response.Status.SERVICE_UNAVAILABLE, "SERVICE_INDISPONIBLE", message);
    }
}
