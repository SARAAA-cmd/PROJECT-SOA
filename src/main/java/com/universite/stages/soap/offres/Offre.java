package com.universite.stages.soap.offres;

import com.universite.stages.common.LocalDateAdapter;
import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDate;

/** Offre de stage publiée par une entreprise partenaire. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Offre", namespace = Namespaces.OFFRES)
public class Offre {

    public static final String ACTIVE = "ACTIVE";
    public static final String SUSPENDUE = "SUSPENDUE";
    public static final String POURVUE = "POURVUE";
    public static final String CLOTUREE = "CLOTUREE";

    private Long id;
    @XmlElement(required = true)
    private Long entrepriseId;
    @XmlElement(required = true)
    private String titre;
    @XmlElement(required = true)
    private String description;
    /** Compétences séparées par des virgules. */
    private String competencesRequises;
    /** Durée du stage en mois. */
    private int dureeMois;
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlSchemaType(name = "date")
    @XmlElement(required = true)
    private LocalDate dateLimite;
    private int nombrePlaces;
    private int placesDisponibles;
    /** Filières ciblées séparées par des virgules (vide = toutes). */
    private String filieresCiblees;
    /** Type de stage : OBSERVATION, OUVRIER_PFA ou PFE (voir TypeStage). */
    @XmlElement(required = true)
    private String typeStage;
    /** Niveaux ouverts, déduits du type (information, ignorée en entrée). */
    private String niveauxAutorises;
    private String lieu;
    private String statut;
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlSchemaType(name = "date")
    private LocalDate datePublication;

    public Offre() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCompetencesRequises() { return competencesRequises; }
    public void setCompetencesRequises(String competencesRequises) { this.competencesRequises = competencesRequises; }
    public int getDureeMois() { return dureeMois; }
    public void setDureeMois(int dureeMois) { this.dureeMois = dureeMois; }
    public LocalDate getDateLimite() { return dateLimite; }
    public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }
    public int getNombrePlaces() { return nombrePlaces; }
    public void setNombrePlaces(int nombrePlaces) { this.nombrePlaces = nombrePlaces; }
    public int getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(int placesDisponibles) { this.placesDisponibles = placesDisponibles; }
    public String getFilieresCiblees() { return filieresCiblees; }
    public void setFilieresCiblees(String filieresCiblees) { this.filieresCiblees = filieresCiblees; }
    public String getTypeStage() { return typeStage; }
    public void setTypeStage(String typeStage) { this.typeStage = typeStage; }
    public String getNiveauxAutorises() { return niveauxAutorises; }
    public void setNiveauxAutorises(String niveauxAutorises) { this.niveauxAutorises = niveauxAutorises; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDate getDatePublication() { return datePublication; }
    public void setDatePublication(LocalDate datePublication) { this.datePublication = datePublication; }
}
