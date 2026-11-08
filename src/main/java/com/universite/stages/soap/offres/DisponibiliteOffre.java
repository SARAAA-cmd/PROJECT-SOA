package com.universite.stages.soap.offres;

import com.universite.stages.soap.commun.Namespaces;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** Disponibilité d'une offre : active, date limite non dépassée, places restantes. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DisponibiliteOffre", namespace = Namespaces.OFFRES)
public class DisponibiliteOffre {

    private Long offreId;
    private boolean existe;
    private boolean active;
    private boolean dateLimiteDepassee;
    private int placesDisponibles;
    private boolean disponible;
    private String message;

    public DisponibiliteOffre() {
    }

    public Long getOffreId() { return offreId; }
    public void setOffreId(Long offreId) { this.offreId = offreId; }
    public boolean isExiste() { return existe; }
    public void setExiste(boolean existe) { this.existe = existe; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDateLimiteDepassee() { return dateLimiteDepassee; }
    public void setDateLimiteDepassee(boolean dateLimiteDepassee) { this.dateLimiteDepassee = dateLimiteDepassee; }
    public int getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(int placesDisponibles) { this.placesDisponibles = placesDisponibles; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
