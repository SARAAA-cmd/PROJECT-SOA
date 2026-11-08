package com.universite.stages.clients;

/** Un service distant n'a pas répondu (panne réseau, délai dépassé, erreur 5xx). */
public class ServiceIndisponibleException extends RuntimeException {

    private final String service;

    public ServiceIndisponibleException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
