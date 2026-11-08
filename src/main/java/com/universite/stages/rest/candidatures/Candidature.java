package com.universite.stages.rest.candidatures;

import java.time.LocalDateTime;

/** Candidature d'un étudiant à une offre de stage. */
public class Candidature {

    public static final String DEPOSEE = "DEPOSEE";
    public static final String EN_COURS_EVALUATION = "EN_COURS_EVALUATION";
    public static final String ACCEPTEE = "ACCEPTEE";
    public static final String REFUSEE = "REFUSEE";
    public static final String ANNULEE = "ANNULEE";

    private Long id;
    private String matricule;
    private String nomEtudiant;
    private Long offreId;
    private String titreOffre;
    private Long entrepriseId;
    private String lettreMotivation;
    private String cvUrl;
    private Integer scoreCompatibilite;
    private String statut;
    private String motif;
    private String processInstanceId;
    private LocalDateTime dateDepot;
    private LocalDateTime dateMiseAJour;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getLettreMotivation() { return lettreMotivation; }
    public void setLettreMotivation(String lettreMotivation) { this.lettreMotivation = lettreMotivation; }
    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }
    public Integer getScoreCompatibilite() { return scoreCompatibilite; }
    public void setScoreCompatibilite(Integer scoreCompatibilite) { this.scoreCompatibilite = scoreCompatibilite; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }
    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }
}
