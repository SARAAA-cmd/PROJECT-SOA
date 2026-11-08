package com.universite.stages.rest.notifications;

import java.time.LocalDateTime;
import java.util.Map;

/** Notification envoyée à un étudiant ou à une entreprise. */
public class Notification {

    private Long id;
    /** Voir {@link TypeNotification}. */
    private String type;
    /** ETUDIANT, ENTREPRISE ou RESPONSABLE. */
    private String destinataireType;
    /** Matricule ou identifiant d'entreprise. */
    private String destinataireId;
    private String email;
    private String sujet;
    private String message;
    private String canal;
    private String statut;
    private Long candidatureId;
    private Long offreId;
    private LocalDateTime dateEnvoi;
    private boolean lue;
    /** Paramètres de gabarit (entrée seulement). */
    private Map<String, String> parametres;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDestinataireType() { return destinataireType; }
    public void setDestinataireType(String destinataireType) { this.destinataireType = destinataireType; }
    public String getDestinataireId() { return destinataireId; }
    public void setDestinataireId(String destinataireId) { this.destinataireId = destinataireId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public Long getCandidatureId() { return candidatureId; }
    public void setCandidatureId(Long candidatureId) { this.candidatureId = candidatureId; }
    public Long getOffreId() { return offreId; }
    public void setOffreId(Long offreId) { this.offreId = offreId; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }
    public Map<String, String> getParametres() { return parametres; }
    public void setParametres(Map<String, String> parametres) { this.parametres = parametres; }
}
