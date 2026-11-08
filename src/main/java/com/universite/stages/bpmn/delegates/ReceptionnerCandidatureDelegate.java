package com.universite.stages.bpmn.delegates;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;

import java.time.LocalDateTime;

/** Étape 1 — Réception : contrôle de forme et initialisation des variables. */
public class ReceptionnerCandidatureDelegate extends AbstractServiceDelegate {

    @Override
    protected void executer(DelegateExecution ex) {
        if (str(ex, "matricule") == null || lng(ex, "offreId") == null) {
            throw new BpmnError("DONNEES_INVALIDES", "Matricule et offre obligatoires");
        }
        ex.setVariable("dateReception", LocalDateTime.now().toString());
        ex.setVariable("recevable", false);
        ex.setVariable("etudiantEligible", false);
        ex.setVariable("offreValide", false);
        ex.setVariable("incidents", "");
        ex.setVariable("nombreDemandesComplement", 0);
        log.info("Candidature reçue : étudiant " + str(ex, "matricule") + " → offre " + lng(ex, "offreId"));
    }
}
