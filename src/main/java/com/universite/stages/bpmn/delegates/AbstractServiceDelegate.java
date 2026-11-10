package com.universite.stages.bpmn.delegates;

import com.universite.stages.common.AppConfig;
import com.universite.stages.clients.Resilience;
import com.universite.stages.clients.ServiceIndisponibleException;
import com.universite.stages.soap.commun.ServiceException;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Base des tâches de service du processus. Une indisponibilité persistante
 * d'un service (après les reprises) est convertie en erreur BPMN
 * {@code ERREUR_TECHNIQUE}, interceptée par les événements de bordure.
 */
public abstract class AbstractServiceDelegate implements JavaDelegate {

    public static final String ERREUR_TECHNIQUE = "ERREUR_TECHNIQUE";
    protected final Logger log = Logger.getLogger(getClass().getName());

    @Override
    public final void execute(DelegateExecution execution) {
        try {
            executer(execution);
        } catch (ServiceIndisponibleException e) {
            String msg = "Service « " + e.getService() + " » indisponible : " + e.getMessage();
            log.warning("[" + execution.getCurrentActivityId() + "] " + msg);
            execution.setVariable("serviceEnErreur", e.getService());
            execution.setVariable("messageErreur", msg);
            execution.setVariable("activiteEnErreur", execution.getCurrentActivityId());
            execution.setVariable("dateErreur", LocalDateTime.now().toString());
            execution.setVariable("tentativesEffectuees", AppConfig.getInt("client.tentatives", 3));
            throw new BpmnError(ERREUR_TECHNIQUE, msg);
        }
    }

    protected abstract void executer(DelegateExecution execution);

    /** Appel protégé par la politique de reprise. */
    protected static <T> T appel(String service, Supplier<T> appel) {
        return Resilience.avecReprise(service, appel);
    }

    /** Appel SOAP pouvant lever une faute métier, convertie en exception non vérifiée. */
    protected static <T> T soap(String service, AppelSoap<T> appel) {
        return Resilience.avecReprise(service, () -> {
            try {
                return appel.executer();
            } catch (ServiceException e) {
                throw new FauteMetier(e);
            }
        });
    }

    @FunctionalInterface
    protected interface AppelSoap<T> {
        T executer() throws ServiceException;
    }

    /** SOAP Fault fonctionnelle (ressource introuvable, règle métier...). */
    protected static class FauteMetier extends RuntimeException {
        private final String code;

        FauteMetier(ServiceException e) {
            super(e.getFaultInfo() != null ? e.getFaultInfo().getMessage() : e.getMessage(), e);
            this.code = e.getFaultInfo() != null ? e.getFaultInfo().getCode() : ServiceException.TECHNIQUE;
        }

        public String getCode() {
            return code;
        }
    }

    protected static String str(DelegateExecution ex, String var) {
        Object v = ex.getVariable(var);
        return v == null ? null : v.toString();
    }

    protected static Long lng(DelegateExecution ex, String var) {
        Object v = ex.getVariable(var);
        if (v == null) {
            return null;
        }
        return v instanceof Number n ? n.longValue() : Long.valueOf(v.toString());
    }
}
