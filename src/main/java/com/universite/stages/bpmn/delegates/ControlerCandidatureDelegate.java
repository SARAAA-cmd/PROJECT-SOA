package com.universite.stages.bpmn.delegates;

import com.fasterxml.jackson.databind.JsonNode;
import com.universite.stages.clients.RestClient;
import com.universite.stages.clients.SoapClients;
import com.universite.stages.common.AppConfig;
import com.universite.stages.soap.offres.DisponibiliteOffre;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.ArrayList;
import java.util.List;

/**
 * Étape 4 — Contrôle de la candidature : places disponibles (SOAP Offres),
 * absence de doublon et plafond de candidatures en cours (REST Candidatures).
 * La date limite est vérifiée avec la compatibilité (étape 3).
 */
public class ControlerCandidatureDelegate extends AbstractServiceDelegate {

    @Override
    protected void executer(DelegateExecution ex) {
        boolean offreValide = Boolean.TRUE.equals(ex.getVariable("offreValide"));
        List<String> motifs = new ArrayList<>();
        if (!offreValide) {
            motifs.add(str(ex, "motifRejet"));
        }
        long offreId = lng(ex, "offreId");
        String matricule = str(ex, "matricule");

        DisponibiliteOffre d = soap("offres", () -> SoapClients.offres().verifierDisponibilite(offreId));
        if (d.isExiste() && d.isActive() && d.getPlacesDisponibles() <= 0) {
            motifs.add("Plus aucune place disponible sur cette offre");
        }

        JsonNode existantes = appel("candidatures", () -> RestClient.get("/api/candidatures?matricule=" + matricule));
        boolean doublon = false;
        int actives = 0;
        if (existantes != null) {
            for (JsonNode c : existantes) {
                String statut = c.path("statut").asText();
                if (c.path("offreId").asLong() == offreId && !"ANNULEE".equals(statut)) {
                    doublon = true;
                }
                if ("DEPOSEE".equals(statut) || "EN_COURS_EVALUATION".equals(statut)) {
                    actives++;
                }
            }
        }
        if (doublon) {
            motifs.add("Une candidature à cette offre a déjà été déposée");
        }
        int max = AppConfig.getInt("regles.max-candidatures-actives", 5);
        if (actives >= max) {
            motifs.add("Nombre maximal de candidatures en cours atteint (" + max + ")");
        }

        ex.setVariable("dateLimiteDepassee", d.isDateLimiteDepassee());
        ex.setVariable("doublon", doublon);
        ex.setVariable("recevable", motifs.isEmpty());
        if (!motifs.isEmpty()) {
            ex.setVariable("motifRejet", String.join(" ; ", motifs));
        }
    }
}
