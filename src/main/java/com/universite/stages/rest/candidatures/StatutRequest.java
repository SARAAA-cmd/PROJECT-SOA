package com.universite.stages.rest.candidatures;

/** Corps de PATCH /candidatures/{id}/statut. */
public record StatutRequest(String statut, String motif) {
}
