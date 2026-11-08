package com.universite.stages.rest.stages;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dossier de stage créé après acceptation d'une candidature. */
public class Stage {

    public static final String PLANIFIE = "PLANIFIE";
    public static final String EN_COURS = "EN_COURS";
    public static final String TERMINE = "TERMINE";
    public static final String INTERROMPU = "INTERROMPU";

    private Long id;
    private Long candidatureId;
    private String matricule;
    private String nomEtudiant;
    private Long offreId;
    private String titreOffre;
    private Long entrepriseId;
    private String nomEntreprise;
    private String sujet;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String tuteurEntreprise;
    private String encadrantAcademique;
    private String statut;
    private LocalDateTime dateCreation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCandidatureId() { return candidatureId; }
    public void setCandidatureId(Long candidatureId) { this.candidatureId = candidatureId; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNomEtudiant() { return nomEtudiant; }
    public void setNomEtudiant(String nomEtudiant) { this.nomEtudiant = nomEtudiant; }
    public Long getOffreId() { return offreId; }
    public void setOffreId(Long offreId) { this.offreId = offreId; }
    public String getTitreOffre() { return titreOffre; }
    public void setTitreOffre(String titreOffre) { this.titreOffre = titreOffre; }
    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public String getTuteurEntreprise() { return tuteurEntreprise; }
    public void setTuteurEntreprise(String tuteurEntreprise) { this.tuteurEntreprise = tuteurEntreprise; }
    public String getEncadrantAcademique() { return encadrantAcademique; }
    public void setEncadrantAcademique(String encadrantAcademique) { this.encadrantAcademique = encadrantAcademique; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
