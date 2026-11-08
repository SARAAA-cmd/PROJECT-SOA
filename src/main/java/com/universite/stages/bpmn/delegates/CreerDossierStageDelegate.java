package com.universite.stages.bpmn.delegates;

import com.fasterxml.jackson.databind.JsonNode;
import com.universite.stages.clients.RestCallException;
import com.universite.stages.clients.RestClient;
import org.flowable.engine.delegate.DelegateExecution;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Étape 9 — Création du dossier de stage (REST Suivi des stages). Idempotent. */
public class CreerDossierStageDelegate extends AbstractServiceDelegate {

    @Override
    protected void executer(DelegateExecution ex) {
        if (ex.getVariable("stageId") != null) {
            return;
        }
        long candidatureId = lng(ex, "candidatureId");
        LocalDate debut = ex.getVariable("dateDebut") != null
                ? LocalDate.parse(str(ex, "dateDebut")) : LocalDate.now().plusDays(30);
        Object duree = ex.getVariable("dureeMois");
        LocalDate fin = ex.getVariable("dateFin") != null
                ? LocalDate.parse(str(ex, "dateFin"))
                : debut.plusMonths(duree == null ? 3 : ((Number) duree).longValue());

        Map<String, Object> corps = new HashMap<>();
        corps.put("candidatureId", candidatureId);
        corps.put("matricule", str(ex, "matricule"));
        corps.put("nomEtudiant", str(ex, "nomEtudiant"));
        corps.put("offreId", lng(ex, "offreId"));
        corps.put("titreOffre", str(ex, "titreOffre"));
        corps.put("entrepriseId", lng(ex, "entrepriseId"));
        corps.put("nomEntreprise", str(ex, "entreprise"));
        corps.put("sujet", str(ex, "titreOffre"));
        corps.put("dateDebut", debut.toString());
        corps.put("dateFin", fin.toString());
        corps.put("tuteurEntreprise", str(ex, "tuteur"));
        corps.put("encadrantAcademique", str(ex, "encadrant"));
        try {
            JsonNode s = appel("stages", () -> RestClient.post("/api/stages", corps));
            ex.setVariable("stageId", s.get("id").asLong());
        } catch (RestCallException e) {
            if (e.getStatus() != 409) {
                throw e;
            }
            // déjà créé lors d'une exécution précédente : on récupère l'existant
            JsonNode l = appel("stages", () -> RestClient.get("/api/stages?candidatureId=" + candidatureId));
            ex.setVariable("stageId", l.get(0).get("id").asLong());
        }
    }
}
