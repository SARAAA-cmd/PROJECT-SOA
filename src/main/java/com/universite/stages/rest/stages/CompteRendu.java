package com.universite.stages.rest.stages;

import java.time.LocalDateTime;

/** Compte rendu périodique d'avancement du stage. */
public record CompteRendu(Long id, Long stageId, String periode, String contenu, String auteur, LocalDateTime dateDepot) {
}
