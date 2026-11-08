package com.universite.stages.rest.candidatures;

import java.time.LocalDateTime;

/** Entrée de l'historique des statuts d'une candidature. */
public record ChangementStatut(String ancienStatut, String nouveauStatut, String commentaire,
                               String auteur, LocalDateTime date) {
}
