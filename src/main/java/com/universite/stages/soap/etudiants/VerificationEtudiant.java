package com.universite.stages.soap.etudiants;

import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** Résultat de la vérification d'existence et de statut d'un étudiant. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VerificationEtudiant", namespace = Namespaces.ETUDIANTS)
public class VerificationEtudiant {

    private String matricule;
    private boolean existe;
    private String statut;
    /** Vrai si l'étudiant peut candidater (existe et statut ACTIF). */
    private boolean eligible;
    private String message;

    public VerificationEtudiant() {
    }

    public VerificationEtudiant(String matricule, boolean existe, String statut, boolean eligible, String message) {
        this.matricule = matricule;
        this.existe = existe;
        this.statut = statut;
        this.eligible = eligible;
        this.message = message;
    }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public boolean isExiste() { return existe; }
    public void setExiste(boolean existe) { this.existe = existe; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
