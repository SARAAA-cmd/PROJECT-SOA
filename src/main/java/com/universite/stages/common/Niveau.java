package com.universite.stages.common;

import java.util.Arrays;
import java.util.List;

/** Années du cycle ingénieur de l'INPT, ordonnées : INE1 < INE2 < INE3. */
public enum Niveau {
    INE1, INE2, INE3;

    public static List<String> valeurs() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
