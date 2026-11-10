package com.universite.stages.bpmn;

import com.universite.stages.clients.RestClient;
import com.universite.stages.common.AppConfig;
import com.universite.stages.rest.commun.ApiException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Façade d'orchestration : démarrage du processus, tâches humaines et suivi d'exécution. */
public class ProcessusService {

    /** Variables du processus exposées par l'API (les autres restent internes). */
    private static final Set<String> VARIABLES_PUBLIQUES = Set.of("matricule", "nomEtudiant", "offreId", "titreOffre",
            "entreprise", "entrepriseId", "candidatureId", "recevable", "motifRejet", "decision", "commentaireDecision",
            "stageId", "scoreCompatibilite", "incidents", "etapeCourante", "messageErreur", "nombreIncidents",
            "statutCandidature", "dateReception", "derniereNotification", "nombreDemandesComplement");

    private ProcessEngine engine() {
        return MoteurBpmn.get();
    }

    /** Étape 1 — dépôt : démarre une instance du processus. */
    public Map<String, Object> deposer(String matricule, Long offreId, String lettre, String cvUrl, String initiateur) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("matricule", matricule);
        vars.put("offreId", offreId);
        vars.put("lettreMotivation", lettre);
        vars.put("cvUrl", cvUrl);
        vars.put("initiateur", initiateur);
        vars.put("delaiRelance", AppConfig.get("processus.delai-relance", "P3D"));
        RuntimeService rs = engine().getRuntimeService();
        ProcessInstance pi = rs.startProcessInstanceByKey(MoteurBpmn.PROCESSUS, matricule + "-" + offreId, vars);
        return instance(pi.getId());
    }

    public List<Map<String, Object>> taches(String typeTache, Long entrepriseId) {
        TaskQuery q = engine().getTaskService().createTaskQuery().active();
        if (typeTache != null) {
            q.taskDefinitionKey(typeTache);
        }
        if (entrepriseId != null) {
            q.processVariableValueEquals("entrepriseId", entrepriseId);
        }
        return q.orderByTaskCreateTime().desc().list().stream().map(this::tache).toList();
    }

    public Map<String, Object> tache(String taskId) {
        Task t = chargerTache(taskId);
        return tache(t);
    }

    /**
     * Prise en charge de l'évaluation par l'entreprise : la tâche lui est
     * attribuée et la candidature passe de DEPOSEE à EN_COURS_EVALUATION
     * (elle ne peut alors plus être annulée par l'étudiant).
     */
    public Map<String, Object> prendreEnCharge(String taskId, String auteur) {
        Task t = chargerTache(taskId);
        if (!"evaluer".equals(t.getTaskDefinitionKey())) {
            throw ApiException.regle("Cette tâche n'est pas une évaluation de candidature");
        }
        if (t.getAssignee() == null) {
            Object candidatureId = engine().getRuntimeService().getVariable(t.getProcessInstanceId(), "candidatureId");
            RestClient.patch("/api/candidatures/" + candidatureId + "/statut",
                    Map.of("statut", "EN_COURS_EVALUATION", "motif", "Examen commencé par " + auteur));
            engine().getTaskService().claim(taskId, auteur);
            engine().getRuntimeService().setVariable(t.getProcessInstanceId(), "statutCandidature", "EN_COURS_EVALUATION");
        }
        return tache(taskId);
    }

    /** Étape 7 — décision de l'entreprise (avec prise en charge implicite si besoin). */
    public Map<String, Object> decider(String taskId, Map<String, String> corps, String auteur) {
        Task t = chargerTache(taskId);
        if (!"evaluer".equals(t.getTaskDefinitionKey())) {
            throw ApiException.regle("Cette tâche n'est pas une évaluation de candidature");
        }
        String decision = corps.get("decision");
        if (decision == null || !Set.of("ACCEPTEE", "REFUSEE", "COMPLEMENT").contains(decision)) {
            throw ApiException.invalide(List.of("decision doit valoir ACCEPTEE, REFUSEE ou COMPLEMENT"));
        }
        if ("COMPLEMENT".equals(decision) && (corps.get("commentaire") == null || corps.get("commentaire").isBlank())) {
            throw ApiException.invalide(List.of("Un commentaire est obligatoire pour demander un complément"));
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("decision", decision);
        vars.put("commentaireDecision", corps.getOrDefault("commentaire", ""));
        vars.put("decideur", auteur);
        if ("ACCEPTEE".equals(decision)) {
            LocalDate debut = date(corps.get("dateDebut"), "dateDebut");
            LocalDate fin = date(corps.get("dateFin"), "dateFin");
            if (debut != null && fin != null && !fin.isAfter(debut)) {
                throw ApiException.invalide(List.of("La date de fin doit être postérieure à la date de début"));
            }
            if (debut != null) {
                vars.put("dateDebut", debut.toString());
            }
            if (fin != null) {
                vars.put("dateFin", fin.toString());
            }
            vars.put("tuteur", corps.get("tuteur"));
            vars.put("encadrant", corps.get("encadrant"));
        }
        if ("COMPLEMENT".equals(decision)) {
            Object n = engine().getRuntimeService().getVariable(t.getProcessInstanceId(), "nombreDemandesComplement");
            vars.put("nombreDemandesComplement", (n == null ? 0 : ((Number) n).intValue()) + 1);
        }
        String pid = t.getProcessInstanceId();
        prendreEnCharge(taskId, auteur);
        engine().getTaskService().complete(taskId, vars);
        return instance(pid);
    }

    /** Gestion d'incident — décision du responsable : relancer l'étape ou abandonner. */
    public Map<String, Object> traiterIncident(String taskId, String action, String auteur) {
        Task t = chargerTache(taskId);
        if (!"traiterIncident".equals(t.getTaskDefinitionKey())) {
            throw ApiException.regle("Cette tâche n'est pas un incident");
        }
        if (!Set.of("RELANCER", "ABANDONNER").contains(action)) {
            throw ApiException.invalide(List.of("action doit valoir RELANCER ou ABANDONNER"));
        }
        String pid = t.getProcessInstanceId();
        engine().getTaskService().complete(taskId, Map.of("actionIncident", action, "incidentOuvert", false,
                "responsableIncident", auteur));
        return instance(pid);
    }

    public List<Map<String, Object>> instances(int limite) {
        return engine().getHistoryService().createHistoricProcessInstanceQuery()
                .processDefinitionKey(MoteurBpmn.PROCESSUS)
                .orderByProcessInstanceStartTime().desc()
                .listPage(0, limite).stream()
                .map(h -> instance(h.getId()))
                .toList();
    }

    /** État complet d'une instance : statut, variables, tâches en attente, trace d'exécution. */
    public Map<String, Object> instance(String pid) {
        HistoryService hs = engine().getHistoryService();
        HistoricProcessInstance h = hs.createHistoricProcessInstanceQuery().processInstanceId(pid).singleResult();
        if (h == null) {
            throw ApiException.introuvable("Instance de processus " + pid + " introuvable");
        }
        Map<String, Object> vars = new LinkedHashMap<>();
        for (HistoricVariableInstance v : hs.createHistoricVariableInstanceQuery().processInstanceId(pid).list()) {
            if (VARIABLES_PUBLIQUES.contains(v.getVariableName()) && v.getValue() != null) {
                vars.put(v.getVariableName(), v.getValue());
            }
        }
        List<Task> enAttente = engine().getTaskService().createTaskQuery().processInstanceId(pid).list();
        List<HistoricActivityInstance> activites = hs.createHistoricActivityInstanceQuery().processInstanceId(pid)
                .orderByHistoricActivityInstanceStartTime().asc().orderByHistoricActivityInstanceId().asc().list();
        // un événement de bordure n'apparaît dans la trace que si son flux sortant a été emprunté
        Set<String> fluxEmpruntes = activites.stream().filter(a -> "sequenceFlow".equals(a.getActivityType()))
                .map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet());
        BpmnModel modele = engine().getRepositoryService().getBpmnModel(h.getProcessDefinitionId());
        List<Map<String, Object>> trace = activites.stream()
                .filter(a -> !"sequenceFlow".equals(a.getActivityType()))
                .filter(a -> !"boundaryEvent".equals(a.getActivityType())
                        || modele.getFlowElement(a.getActivityId()) instanceof FlowNode n
                        && n.getOutgoingFlows().stream().anyMatch(f -> fluxEmpruntes.contains(f.getId())))
                .map(ProcessusService::etape).toList();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("processInstanceId", pid);
        r.put("statut", statut(h, enAttente, vars));
        r.put("debut", h.getStartTime());
        if (h.getEndTime() != null) {
            r.put("fin", h.getEndTime());
            r.put("evenementFin", h.getEndActivityId());
        }
        if (h.getDeleteReason() != null) {
            r.put("raisonArret", h.getDeleteReason());
        }
        r.put("variables", vars);
        r.put("tachesEnAttente", enAttente.stream().map(this::tache).toList());
        r.put("activitesActives", h.getEndTime() == null
                ? engine().getRuntimeService().getActiveActivityIds(pid) : List.of());
        r.put("trace", trace);
        return r;
    }

    public String definitionXml() {
        var repo = engine().getRepositoryService();
        var def = repo.createProcessDefinitionQuery().processDefinitionKey(MoteurBpmn.PROCESSUS).latestVersion().singleResult();
        try (var in = repo.getResourceAsStream(def.getDeploymentId(), def.getResourceName())) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String statut(HistoricProcessInstance h, List<Task> enAttente, Map<String, Object> vars) {
        if (h.getDeleteReason() != null) {
            return "INTERROMPU";
        }
        if (h.getEndTime() != null) {
            return switch (String.valueOf(h.getEndActivityId())) {
                case "finRejet" -> "REJETEE";
                case "finAbandon" -> "ABANDONNEE";
                default -> "ACCEPTEE".equals(vars.get("decision")) ? "ACCEPTEE" : "REFUSEE";
            };
        }
        if (enAttente.stream().anyMatch(t -> "traiterIncident".equals(t.getTaskDefinitionKey()))) {
            return "INCIDENT_TECHNIQUE";
        }
        if (enAttente.stream().anyMatch(t -> "evaluer".equals(t.getTaskDefinitionKey()))) {
            return "EN_ATTENTE_DECISION";
        }
        return "EN_COURS";
    }

    private static Map<String, Object> etape(HistoricActivityInstance a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activite", a.getActivityId());
        m.put("nom", a.getActivityName());
        m.put("type", a.getActivityType());
        m.put("debut", a.getStartTime());
        if (a.getEndTime() != null) {
            m.put("fin", a.getEndTime());
            m.put("dureeMs", a.getDurationInMillis());
        }
        return m;
    }

    private Map<String, Object> tache(Task t) {
        Map<String, Object> vars = engine().getRuntimeService().getVariables(t.getProcessInstanceId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("nom", t.getName());
        m.put("type", t.getTaskDefinitionKey());
        m.put("creeLe", t.getCreateTime());
        if (t.getAssignee() != null) {
            m.put("priseEnChargePar", t.getAssignee());
        }
        m.put("processInstanceId", t.getProcessInstanceId());
        Map<String, Object> ctx = new LinkedHashMap<>();
        for (String k : List.of("candidatureId", "matricule", "nomEtudiant", "titreOffre", "offreId", "entrepriseId",
                "entreprise", "scoreCompatibilite", "lettreMotivation", "cvUrl", "etapeCourante", "messageErreur",
                "commentaireDecision", "nombreDemandesComplement", "serviceEnErreur", "activiteEnErreur", "dateErreur",
                "tentativesEffectuees")) {
            if (vars.get(k) != null) {
                ctx.put(k, vars.get(k));
            }
        }
        m.put("contexte", ctx);
        return m;
    }

    private Task chargerTache(String taskId) {
        TaskService ts = engine().getTaskService();
        Task t = ts.createTaskQuery().taskId(taskId).singleResult();
        if (t == null) {
            throw ApiException.introuvable("Tâche " + taskId + " introuvable ou déjà traitée");
        }
        return t;
    }

    private static LocalDate date(String v, String champ) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw ApiException.invalide(List.of(champ + " doit être au format AAAA-MM-JJ"));
        }
    }
}
