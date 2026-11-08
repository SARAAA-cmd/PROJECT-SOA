package com.universite.stages.bpmn.delegates;

import com.universite.stages.clients.RestClient;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.HashMap;
import java.util.Map;

/** Mise à jour du statut de la candidature (REST PATCH). Champ injecté : statut. */
public class MajStatutCandidatureDelegate extends AbstractServiceDelegate {

    private Expression statut;

    @Override
    protected void executer(DelegateExecution ex) {
        String nouveau = (String) statut.getValue(ex);
        Map<String, Object> corps = new HashMap<>();
        corps.put("statut", nouveau);
        corps.put("motif", str(ex, "commentaireDecision"));
        appel("candidatures", () -> RestClient.patch("/api/candidatures/" + lng(ex, "candidatureId") + "/statut", corps));
        ex.setVariable("statutCandidature", nouveau);
    }
}
