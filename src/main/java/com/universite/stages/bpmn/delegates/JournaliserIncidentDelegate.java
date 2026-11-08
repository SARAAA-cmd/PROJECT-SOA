package com.universite.stages.bpmn.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import java.util.logging.Logger;

/** Gestion des erreurs — journalisation de l'incident technique avant traitement manuel. */
public class JournaliserIncidentDelegate implements JavaDelegate {

    private static final Logger LOG = Logger.getLogger(JournaliserIncidentDelegate.class.getName());

    @Override
    public void execute(DelegateExecution ex) {
        Object n = ex.getVariable("nombreIncidents");
        int nombre = n == null ? 1 : ((Number) n).intValue() + 1;
        ex.setVariable("nombreIncidents", nombre);
        ex.setVariable("incidentOuvert", true);
        LOG.severe("INCIDENT n°" + nombre + " processus " + ex.getProcessInstanceId() + " étape "
                + ex.getVariable("etapeCourante") + " : " + ex.getVariable("messageErreur"));
    }
}
