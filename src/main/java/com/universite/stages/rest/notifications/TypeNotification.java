package com.universite.stages.rest.notifications;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Types de notification et gabarits de messages associés ({param} remplacé). */
public enum TypeNotification {

    CONFIRMATION_DEPOT("Confirmation de dépôt de votre candidature",
            "Bonjour {nomEtudiant},\nVotre candidature n°{candidatureId} à l'offre « {titreOffre} » ({entreprise}) "
                    + "a bien été enregistrée. Elle va être transmise à l'entreprise pour évaluation."),
    RECEPTION_CANDIDATURE("Nouvelle candidature reçue",
            "Bonjour,\nUne nouvelle candidature (n°{candidatureId}) de {nomEtudiant} a été reçue pour votre offre "
                    + "« {titreOffre} ». Merci de l'examiner depuis votre espace entreprise."),
    REJET_CANDIDATURE("Candidature non recevable",
            "Bonjour {nomEtudiant},\nVotre candidature à l'offre « {titreOffre} » n'a pas pu être enregistrée.\n"
                    + "Motif : {motif}"),
    ACCEPTATION("Votre candidature a été acceptée",
            "Félicitations {nomEtudiant} !\nL'entreprise {entreprise} a accepté votre candidature à l'offre "
                    + "« {titreOffre} ». Votre dossier de stage a été créé. {commentaire}"),
    REFUS("Réponse à votre candidature",
            "Bonjour {nomEtudiant},\nNous sommes au regret de vous informer que votre candidature à l'offre "
                    + "« {titreOffre} » n'a pas été retenue. {commentaire}"),
    DEMANDE_COMPLEMENT("Demande de complément d'information",
            "Bonjour {nomEtudiant},\nL'entreprise {entreprise} souhaite des informations complémentaires sur votre "
                    + "candidature à l'offre « {titreOffre} » : {commentaire}"),
    RAPPEL_DATE_LIMITE("Rappel : date limite de candidature proche",
            "Bonjour {nomEtudiant},\nL'offre « {titreOffre} » correspondant à votre profil clôture ses candidatures "
                    + "le {dateLimite}. Pensez à postuler !"),
    RELANCE_DECISION("Relance : candidature en attente de décision",
            "Bonjour,\nLa candidature n°{candidatureId} de {nomEtudiant} pour l'offre « {titreOffre} » attend "
                    + "toujours votre décision."),
    INCIDENT_TRAITEMENT("Incident lors du traitement de votre candidature",
            "Bonjour {nomEtudiant},\nLe traitement de votre candidature à l'offre « {titreOffre} » a été interrompu "
                    + "suite à un incident technique. {motif}"),
    ANNULATION("Annulation de candidature",
            "Bonjour,\nLa candidature n°{candidatureId} à l'offre « {titreOffre} » a été annulée."),
    LIBRE("{sujet}", "{message}");

    private static final Pattern PARAM = Pattern.compile("\\{(\\w+)}");

    private final String sujet;
    private final String gabarit;

    TypeNotification(String sujet, String gabarit) {
        this.sujet = sujet;
        this.gabarit = gabarit;
    }

    public String sujet(Map<String, String> p) {
        return remplir(sujet, p);
    }

    public String message(Map<String, String> p) {
        return remplir(gabarit, p);
    }

    private static String remplir(String modele, Map<String, String> p) {
        Matcher m = PARAM.matcher(modele);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = p == null ? null : p.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : v));
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }
}
