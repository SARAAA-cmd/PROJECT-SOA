package com.universite.stages.bpmn;

import com.universite.stages.common.AppConfig;
import com.universite.stages.rest.candidatures.CandidatureEvenements;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;

import java.util.logging.Logger;

/** Moteur BPMN 2.0 Flowable embarqué : configuration et déploiement du processus. */
public final class MoteurBpmn {

    public static final String PROCESSUS = "traitementCandidature";
    private static final Logger LOG = Logger.getLogger(MoteurBpmn.class.getName());
    private static ProcessEngine engine;

    private MoteurBpmn() {
    }

    public static synchronized ProcessEngine demarrer() {
        if (engine != null) {
            return engine;
        }
        StandaloneProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration();
        cfg.setJdbcUrl(AppConfig.get("bpmn.db.url"))
                .setJdbcUsername(AppConfig.get("bpmn.db.user", "sa"))
                .setJdbcPassword(AppConfig.get("bpmn.db.password", ""))
                .setJdbcDriver(AppConfig.get("bpmn.db.url").startsWith("jdbc:postgresql") ? "org.postgresql.Driver" : "org.h2.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setAsyncExecutorActivate(true);
        cfg.setDisableIdmEngine(true);
        cfg.setDisableEventRegistry(true);
        // acquisition rapide des minuteurs (relance des entreprises)
        cfg.setAsyncExecutorDefaultTimerJobAcquireWaitTime(1000);
        cfg.setAsyncExecutorDefaultAsyncJobAcquireWaitTime(1000);
        engine = cfg.buildProcessEngine();
        engine.getRepositoryService().createDeployment()
                .name("Gestion des stages")
                .addClasspathResource("processes/candidature-stage.bpmn20.xml")
                .enableDuplicateFiltering()
                .deploy();
        // une candidature annulée interrompt son processus
        CandidatureEvenements.surAnnulation(c -> {
            if (c.getProcessInstanceId() != null && engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(c.getProcessInstanceId()).count() > 0) {
                engine.getRuntimeService().deleteProcessInstance(c.getProcessInstanceId(),
                        "Candidature annulée par l'étudiant");
            }
        });
        LOG.info("Moteur BPMN démarré, processus « " + PROCESSUS + " » déployé");
        return engine;
    }

    public static ProcessEngine get() {
        if (engine == null) {
            throw new IllegalStateException("Moteur BPMN non démarré");
        }
        return engine;
    }

    public static synchronized void arreter() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }
}
