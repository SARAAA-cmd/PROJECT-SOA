package com.universite.stages.soap.entreprises;

import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** Résultat de la vérification du référencement d'une entreprise. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VerificationEntreprise", namespace = Namespaces.ENTREPRISES)
public class VerificationEntreprise {

    private Long entrepriseId;
    private boolean existe;
    private boolean referencee;
    private String raisonSociale;
    private String message;

    public VerificationEntreprise() {
    }

    public VerificationEntreprise(Long entrepriseId, boolean existe, boolean referencee, String raisonSociale, String message) {
        this.entrepriseId = entrepriseId;
        this.existe = existe;
        this.referencee = referencee;
        this.raisonSociale = raisonSociale;
        this.message = message;
    }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }
    public boolean isExiste() { return existe; }
    public void setExiste(boolean existe) { this.existe = existe; }
    public boolean isReferencee() { return referencee; }
    public void setReferencee(boolean referencee) { this.referencee = referencee; }
    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
