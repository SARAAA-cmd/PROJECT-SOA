package com.universite.stages.rest.notifications;

import com.universite.stages.common.AppConfig;
import com.universite.stages.common.Masquage;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Canal e-mail : envoi SMTP (Jakarta Mail) d'un message texte + HTML.
 * En démonstration, le serveur SMTP est Mailpit, qui capture les messages
 * dans une boîte consultable sur http://localhost:8025.
 */
class EmailSender {

    private static final Logger LOG = Logger.getLogger(EmailSender.class.getName());

    boolean actif() {
        return Boolean.parseBoolean(AppConfig.get("mail.actif", "false"));
    }

    /** @return vrai si le serveur SMTP a accepté le message. */
    boolean envoyer(String destinataire, String sujet, String texte) {
        Properties p = new Properties();
        p.put("mail.smtp.host", AppConfig.get("mail.smtp.hote", "localhost"));
        p.put("mail.smtp.port", AppConfig.get("mail.smtp.port", "1025"));
        String timeout = AppConfig.get("mail.timeout-ms", "3000");
        p.put("mail.smtp.connectiontimeout", timeout);
        p.put("mail.smtp.timeout", timeout);
        try {
            MimeMessage m = new MimeMessage(Session.getInstance(p));
            m.setFrom(expediteur());
            m.setRecipient(Message.RecipientType.TO, new InternetAddress(destinataire));
            m.setSubject(sujet, "UTF-8");
            m.setSentDate(new Date());
            MimeBodyPart partieTexte = new MimeBodyPart();
            partieTexte.setText(texte, "UTF-8");
            MimeBodyPart partieHtml = new MimeBodyPart();
            partieHtml.setContent(html(sujet, texte), "text/html; charset=UTF-8");
            MimeMultipart alternatives = new MimeMultipart("alternative");
            alternatives.addBodyPart(partieTexte);
            alternatives.addBodyPart(partieHtml);
            m.setContent(alternatives);
            Transport.send(m);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOG.warning("E-mail à " + Masquage.email(destinataire) + " non envoyé : " + e.getMessage());
            return false;
        }
    }

    private static InternetAddress expediteur() throws UnsupportedEncodingException, MessagingException {
        String exp = AppConfig.get("mail.expediteur", "stages@inpt-stages.example");
        InternetAddress[] a = InternetAddress.parse(exp);
        return new InternetAddress(a[0].getAddress(), a[0].getPersonal(), "UTF-8");
    }

    /** Gabarit HTML sobre aux couleurs de la plateforme. */
    static String html(String sujet, String texte) {
        StringBuilder corps = new StringBuilder();
        for (String ligne : texte.split("\n")) {
            corps.append("<p style=\"margin:0 0 12px\">").append(echapper(ligne)).append("</p>");
        }
        return """
                <!doctype html><html lang="fr"><body style="margin:0;background:#eef2f8;font-family:Segoe UI,Arial,sans-serif;color:#1d2433">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"><tr><td align="center" style="padding:28px 12px">
                <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:12px;overflow:hidden">
                <tr><td style="background:#0B3D91;color:#ffffff;padding:18px 26px;font-size:15px;font-weight:700;letter-spacing:.3px">
                INPT · Plateforme de gestion des stages</td></tr>
                <tr><td style="padding:26px 26px 8px"><h1 style="font-size:19px;margin:0 0 16px;color:#0B3D91">%s</h1>%s</td></tr>
                <tr><td style="padding:8px 26px 26px;font-size:12px;color:#6b7489;border-top:1px solid #e3e7ef">
                Message automatique envoyé par le bureau des stages. Merci de ne pas y répondre.</td></tr>
                </table></td></tr></table></body></html>
                """.formatted(echapper(sujet), corps);
    }

    private static String echapper(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
