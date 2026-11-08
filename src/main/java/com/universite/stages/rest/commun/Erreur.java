package com.universite.stages.rest.commun;

import java.time.OffsetDateTime;
import java.util.List;

/** Corps JSON uniforme des réponses d'erreur. */
public record Erreur(int status, String erreur, String message, List<String> details, OffsetDateTime horodatage) {

    public Erreur(int status, String erreur, String message, List<String> details) {
        this(status, erreur, message, details == null || details.isEmpty() ? null : details, OffsetDateTime.now());
    }
}
