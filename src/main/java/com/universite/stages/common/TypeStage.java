package com.universite.stages.common;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Types de stage de l'école et niveaux (années du cycle ingénieur) auxquels chacun est ouvert.
 * Les niveaux autorisés découlent du type : l'entreprise ne choisit que le type de son offre.
 */
public enum TypeStage {
    OBSERVATION("Stage d'observation (initiation)", "stage d'observation", List.of("INE1"), "de 1re année (INE1)"),
    OUVRIER_PFA("Stage ouvrier / PFA", "stage ouvrier / PFA", List.of("INE1", "INE2"), "de 1re et 2e année (INE1, INE2)"),
    PFE("Stage PFE", "stage PFE", List.of("INE3"), "de 3e année (INE3)");

    private final String libelle;
    private final String nom;
    private final List<String> niveaux;
    private final String publicVise;

    TypeStage(String libelle, String nom, List<String> niveaux, String publicVise) {
        this.libelle = libelle;
        this.nom = nom;
        this.niveaux = niveaux;
        this.publicVise = publicVise;
    }

    public String libelle() { return libelle; }

    public List<String> niveaux() { return niveaux; }

    /** Le niveau de l'étudiant fait-il partie des niveaux ouverts à ce type de stage ? */
    public boolean autorise(String niveau) {
        return niveau != null && niveaux.contains(niveau);
    }

    /** Motif clair, repris tel quel dans le rejet du processus BPMN. */
    public String motifRefus() {
        return "Cette offre de " + nom + " est réservée aux étudiants " + publicVise + ".";
    }

    public static List<String> valeurs() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    public static Optional<TypeStage> de(String code) {
        return Arrays.stream(values()).filter(t -> t.name().equals(code)).findFirst();
    }
}
