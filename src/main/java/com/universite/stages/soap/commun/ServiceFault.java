package com.universite.stages.soap.commun;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/** Détail structuré d'une SOAP Fault (élément &lt;detail&gt;). */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceFault", namespace = Namespaces.COMMUN)
public class ServiceFault {

    @XmlElement(required = true)
    private String code;
    @XmlElement(required = true)
    private String message;
    @XmlElement(name = "detail")
    private List<String> details = new ArrayList<>();

    public ServiceFault() {
    }

    public ServiceFault(String code, String message, List<String> details) {
        this.code = code;
        this.message = message;
        if (details != null) {
            this.details = new ArrayList<>(details);
        }
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
}
