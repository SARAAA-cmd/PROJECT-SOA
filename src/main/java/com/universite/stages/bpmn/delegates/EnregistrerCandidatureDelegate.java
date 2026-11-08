package com.universite.stages.bpmn.delegates;

import com.fasterxml.jackson.databind.JsonNode;
import com.universite.stages.clients.RestClient;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.HashMap;
import java.util.Map;

/** Étape 5 (branche favorable) — Enregistrement de la candidature (REST Candidatures). Idempotent. */
public class EnregistrerCandidatureDelegate extends AbstractServiceDelegate {

    @Override
    protected void executer(DelegateExecution ex) {
        if (ex.getVariable("candidatureId") != null) {
            return; // déjà enregistrée (reprise après incident)
        }
        Map<String, Object> corps = new HashMap<>();
        corps.put("matricule", str(ex, "matricule"));
        corps.put("nomEtudiant", str(ex, "nomEtudiant"));
        corps.put("offreId", lng(ex, "offreId"));
        corps.put("titreOffre", str(ex, "titreOffre"));
        corps.put("entrepriseId", lng(ex, "entrepriseId"));
        corps.put("lettreMotivation", str(ex, "lettreMotivation"));
        corps.put("cvUrl", str(ex, "cvUrl"));
        corps.put("scoreCompatibilite", ex.getVariable("scoreCompatibilite"));
        corps.put("processInstanceId", ex.getProcessInstanceId());
        JsonNode c = appel("candidatures", () -> RestClient.post("/api/candidatures", corps));
        ex.setVariable("candidatureId", c.get("id").asLong());
    }
}
