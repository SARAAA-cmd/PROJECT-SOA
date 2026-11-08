package com.universite.stages.soap.etudiants;

import com.universite.stages.common.LocalDateAdapter;
import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDate;

/** Fiche étudiant échangée par le service SOAP de gestion des étudiants. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Etudiant", namespace = Namespaces.ETUDIANTS)
public class Etudiant {

    /** Statuts possibles d'un étudiant. */
    public static final String ACTIF = "ACTIF";
    public static final String SUSPENDU = "SUSPENDU";
    public static final String EN_STAGE = "EN_STAGE";
    public static final String DIPLOME = "DIPLOME";

    @XmlElement(required = true)
    private String matricule;
    @XmlElement(required = true)
    private String nom;
    @XmlElement(required = true)
    private String prenom;
    @XmlElement(required = true)
    private String email;
    private String telephone;
    private String adresse;
    @XmlElement(required = true)
    private String filiere;
    /** L1, L2, L3, M1, M2 ou D. */
    @XmlElement(required = true)
    private String niveau;
    /** Compétences séparées par des virgules (ex. "Java, SQL, UML"). */
    private String competences;
    private String statut;
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlSchemaType(name = "date")
    private LocalDate dateInscription;

    public Etudiant() {
    }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public String getCompetences() { return competences; }
    public void setCompetences(String competences) { this.competences = competences; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
