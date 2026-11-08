package com.universite.stages.soap.offres;

import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/** Compatibilité entre une offre et le profil académique d'un étudiant. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Compatibilite", namespace = Namespaces.OFFRES)
public class Compatibilite {

    private Long offreId;
    /** Vrai si filière, niveau et date limite sont respectés (critères bloquants). */
    private boolean compatible;
    private boolean filiereCompatible;
    private boolean niveauCompatible;
    private boolean dateLimiteRespectee;
    /** Pourcentage des compétences requises couvertes par l'étudiant (0-100). */
    private int scoreCompetences;
    @XmlElement(name = "competenceManquante")
    private List<String> competencesManquantes = new ArrayList<>();
    @XmlElement(name = "motif")
    private List<String> motifs = new ArrayList<>();

    public Compatibilite() {
    }

    public Long getOffreId() { return offreId; }
    public void setOffreId(Long offreId) { this.offreId = offreId; }
    public boolean isCompatible() { return compatible; }
    public void setCompatible(boolean compatible) { this.compatible = compatible; }
    public boolean isFiliereCompatible() { return filiereCompatible; }
    public void setFiliereCompatible(boolean filiereCompatible) { this.filiereCompatible = filiereCompatible; }
    public boolean isNiveauCompatible() { return niveauCompatible; }
    public void setNiveauCompatible(boolean niveauCompatible) { this.niveauCompatible = niveauCompatible; }
    public boolean isDateLimiteRespectee() { return dateLimiteRespectee; }
    public void setDateLimiteRespectee(boolean dateLimiteRespectee) { this.dateLimiteRespectee = dateLimiteRespectee; }
    public int getScoreCompetences() { return scoreCompetences; }
    public void setScoreCompetences(int scoreCompetences) { this.scoreCompetences = scoreCompetences; }
    public List<String> getCompetencesManquantes() { return competencesManquantes; }
    public void setCompetencesManquantes(List<String> competencesManquantes) { this.competencesManquantes = competencesManquantes; }
    public List<String> getMotifs() { return motifs; }
    public void setMotifs(List<String> motifs) { this.motifs = motifs; }
}
