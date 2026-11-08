package com.universite.stages.soap.commun;

import jakarta.xml.ws.WebFault;

import java.util.List;

/**
 * Exception métier déclarée dans les WSDL : elle est convertie en SOAP Fault
 * portant un {@link ServiceFault} (code + message + détails).
 */
@WebFault(name = "ServiceFault", targetNamespace = Namespaces.COMMUN)
public class ServiceException extends Exception {

    public static final String INTROUVABLE = "RESSOURCE_INTROUVABLE";
    public static final String INVALIDE = "DONNEES_INVALIDES";
    public static final String CONFLIT = "CONFLIT";
    public static final String ACCES_REFUSE = "ACCES_REFUSE";
    public static final String REGLE_METIER = "REGLE_METIER";
    public static final String TECHNIQUE = "ERREUR_TECHNIQUE";

    private final ServiceFault faultInfo;

    public ServiceException(String message, ServiceFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public ServiceException(String message, ServiceFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public ServiceFault getFaultInfo() {
        return faultInfo;
    }

    public static ServiceException of(String code, String message) {
        return new ServiceException(message, new ServiceFault(code, message, null));
    }

    public static ServiceException of(String code, String message, List<String> details) {
        return new ServiceException(message, new ServiceFault(code, message, details));
    }

    public static ServiceException introuvable(String message) {
        return of(INTROUVABLE, message);
    }

    public static ServiceException invalide(List<String> erreurs) {
        return of(INVALIDE, "Données invalides", erreurs);
    }
}
