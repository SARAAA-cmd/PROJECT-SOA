package com.universite.stages.bpmn.delegates;

import com.universite.stages.clients.RestClient;
import com.universite.stages.clients.ServiceIndisponibleException;
import com.universite.stages.clients.RestCallException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Envoi d'une notification (REST Notifications). Champs injectés : type,
 * destinataire (ETUDIANT | ENTREPRISE).
 * <p>
 * Une notification est « non bloquante » : en cas d'échec persistant, l'incident
 * est consigné dans la variable {@code incidents} et le processus continue.
 */
public class NotifierDelegate extends AbstractServiceDelegate {

    static final String MOTIF_INCIDENT = "Vous pouvez déposer à nouveau votre candidature ; "
            + "le bureau des stages reste à votre disposition pour toute question.";

    private Expression type;
    private Expression destinataire;

    @Override
    protected void executer(DelegateExecution ex) {
        String t = (String) type.getValue(ex);
        String dest = (String) destinataire.getValue(ex);
        boolean etudiant = "ETUDIANT".equals(dest);

        Map<String, String> params = new HashMap<>();
        put(params, "nomEtudiant", str(ex, "nomEtudiant"));
        put(params, "titreOffre", str(ex, "titreOffre") != null ? str(ex, "titreOffre") : "n°" + lng(ex, "offreId"));
        put(params, "entreprise", str(ex, "entreprise"));
        // le détail technique (messageErreur) reste réservé à la supervision : l'étudiant reçoit un texte neutre
        put(params, "motif", "INCIDENT_TRAITEMENT".equals(t) ? MOTIF_INCIDENT : str(ex, "motifRejet"));
        put(params, "commentaire", str(ex, "commentaireDecision"));

        Map<String, Object> corps = new HashMap<>();
        corps.put("type", t);
        corps.put("destinataireType", dest);
        corps.put("destinataireId", etudiant ? str(ex, "matricule") : String.valueOf(lng(ex, "entrepriseId")));
        corps.put("email", etudiant ? str(ex, "emailEtudiant") : str(ex, "emailEntreprise"));
        corps.put("candidatureId", lng(ex, "candidatureId"));
        corps.put("offreId", lng(ex, "offreId"));
        corps.put("parametres", params);
        try {
            appel("notifications", () -> RestClient.post("/api/notifications", corps));
            ex.setVariable("derniereNotification", t);
        } catch (ServiceIndisponibleException | RestCallException e) {
            String incident = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + " notification " + t + " non envoyée (" + e.getMessage() + ")";
            log.warning(incident);
            String precedents = str(ex, "incidents");
            ex.setVariable("incidents", precedents == null || precedents.isEmpty() ? incident : precedents + "\n" + incident);
        }
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (v != null) {
            m.put(k, v);
        }
    }
}
