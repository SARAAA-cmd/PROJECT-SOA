package com.universite.stages.rest.stages;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Évaluation du stagiaire (MI_PARCOURS ou FINALE), notée sur 20. */
public record Evaluation(Long id, Long stageId, String type, String evaluateur, BigDecimal note,
                         String appreciation, LocalDateTime dateEvaluation) {
}
