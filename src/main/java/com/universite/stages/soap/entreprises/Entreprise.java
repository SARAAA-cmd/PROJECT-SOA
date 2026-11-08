package com.universite.stages.soap.entreprises;

import com.universite.stages.common.LocalDateAdapter;
import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDate;

/** Entreprise partenaire. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Entreprise", namespace = Namespaces.ENTREPRISES)
public class Entreprise {

    private Long id;
    @XmlElement(required = true)
    private String raisonSociale;
    @XmlElement(required = true)
    private String secteurActivite;
    private String adresse;
    private String ville;
    @XmlElement(required = true)
    private String email;
    private String telephone;
    private String siteWeb;
    /** Responsable de contact. */
    @XmlElement(required = true)
    private String contactNom;
    private String contactFonction;
    @XmlElement(required = true)
    private String contactEmail;
    private String contactTelephone;
    /** Vrai si l'entreprise est référencée (convention de partenariat signée). */
    private boolean referencee;
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlSchemaType(name = "date")
    private LocalDate dateReferencement;

    public Entreprise() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }
    public String getSecteurActivite() { return secteurActivite; }
    public void setSecteurActivite(String secteurActivite) { this.secteurActivite = secteurActivite; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }
    public String getContactNom() { return contactNom; }
    public void setContactNom(String contactNom) { this.contactNom = contactNom; }
    public String getContactFonction() { return contactFonction; }
    public void setContactFonction(String contactFonction) { this.contactFonction = contactFonction; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactTelephone() { return contactTelephone; }
    public void setContactTelephone(String contactTelephone) { this.contactTelephone = contactTelephone; }
    public boolean isReferencee() { return referencee; }
    public void setReferencee(boolean referencee) { this.referencee = referencee; }
    public LocalDate getDateReferencement() { return dateReferencement; }
    public void setDateReferencement(LocalDate dateReferencement) { this.dateReferencement = dateReferencement; }
}
