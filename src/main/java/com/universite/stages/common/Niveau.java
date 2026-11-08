package com.universite.stages.common;

import java.util.Arrays;
import java.util.List;

/** Niveaux d'études ordonnés (LMD). */
public enum Niveau {
    L1, L2, L3, M1, M2, D;

    public static List<String> valeurs() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    public static boolean atteint(String niveauEtudiant, String niveauMinimum) {
        if (niveauMinimum == null || niveauMinimum.isBlank()) {
            return true;
        }
        return valueOf(niveauEtudiant).ordinal() >= valueOf(niveauMinimum).ordinal();
    }
}
