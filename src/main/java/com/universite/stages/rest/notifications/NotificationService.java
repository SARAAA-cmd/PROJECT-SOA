package com.universite.stages.rest.notifications;

import com.universite.stages.clients.Resilience;
import com.universite.stages.clients.SoapClients;
import com.universite.stages.common.Masquage;
import com.universite.stages.common.Validation;
import com.universite.stages.soap.commun.ServiceException;
import com.universite.stages.soap.etudiants.Etudiant;
import com.universite.stages.soap.offres.Compatibilite;
import com.universite.stages.soap.offres.Offre;
import com.universite.stages.soap.offres.OffreService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Logique d'envoi : rendu du gabarit, envoi sur le canal (e-mail simulé :
 * journalisé et archivé), et campagne de rappels des dates limites.
 */
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());
    static final List<String> DESTINATAIRES = List.of("ETUDIANT", "ENTREPRISE", "RESPONSABLE");

    private final NotificationDao dao = new NotificationDao();
    private final EmailSender email = new EmailSender();

    public Notification envoyer(Notification n) {
        Validation v = Validation.create()
                .obligatoire("type", n.getType())
                .dansListe("type", n.getType(), Arrays.stream(TypeNotification.values()).map(Enum::name).toList())
                .obligatoire("destinataireType", n.getDestinataireType())
                .dansListe("destinataireType", n.getDestinataireType(), DESTINATAIRES)
                .obligatoire("destinataireId", n.getDestinataireId())
                .email("email", n.getEmail());
        if (!v.estValide()) {
            throw com.universite.stages.rest.commun.ApiException.invalide(v.erreurs());
        }
        TypeNotification type = TypeNotification.valueOf(n.getType());
        Map<String, String> p = new HashMap<>(n.getParametres() == null ? Map.of() : n.getParametres());
        if (n.getCandidatureId() != null) {
            p.putIfAbsent("candidatureId", String.valueOf(n.getCandidatureId()));
        }
        if (type == TypeNotification.LIBRE) {
            p.putIfAbsent("sujet", n.getSujet());
            p.putIfAbsent("message", n.getMessage());
        }
        n.setSujet(type.sujet(p));
        n.setMessage(type.message(p));
        n.setCanal("EMAIL");
        n.setDateEnvoi(LocalDateTime.now());
        n.setLue(false);
        n.setParametres(null);
        // Envoi SMTP réel si le canal est actif ; la notification reste archivée et
        // consultable dans l'application quel que soit le résultat de l'envoi.
        if (n.getEmail() == null || n.getEmail().isBlank()) {
            n.setStatut("ARCHIVEE");
        } else if (!email.actif()) {
            n.setStatut("JOURNALISEE");
        } else {
            n.setStatut(email.envoyer(n.getEmail(), n.getSujet(), n.getMessage()) ? "ENVOYEE" : "ECHEC_ENVOI");
        }
        LOG.info(() -> "[EMAIL " + n.getStatut() + "] à " + Masquage.email(n.getEmail()) + " ("
                + n.getDestinataireType() + " " + n.getDestinataireId() + ") — " + n.getSujet());
        return dao.inserer(n);
    }

    /**
     * Rappels des dates limites : pour chaque offre active clôturant dans
     * {@code joursAvant} jours, prévient les étudiants actifs au profil
     * compatible (composition des services SOAP Offres et Étudiants).
     */
    public Map<String, Object> envoyerRappels(int joursAvant) {
        OffreService offres = SoapClients.offres();
        List<Offre> expirant = Resilience.avecReprise("offres", () -> appel(() -> offres.listerOffresExpirantBientot(joursAvant)));
        List<Etudiant> etudiants = Resilience.avecReprise("etudiants",
                () -> appel(() -> SoapClients.etudiants().listerEtudiants(null, Etudiant.ACTIF)));
        int envoyes = 0;
        for (Offre o : expirant) {
            for (Etudiant e : etudiants) {
                if (dao.rappelDejaEnvoye(e.getMatricule(), o.getId())) {
                    continue;
                }
                Compatibilite c = appel(() -> offres.verifierCompatibilite(o.getId(), e.getFiliere(), e.getNiveau(),
                        e.getCompetences()));
                if (!c.isCompatible()) {
                    continue;
                }
                Notification n = new Notification();
                n.setType(TypeNotification.RAPPEL_DATE_LIMITE.name());
                n.setDestinataireType("ETUDIANT");
                n.setDestinataireId(e.getMatricule());
                n.setEmail(e.getEmail());
                n.setOffreId(o.getId());
                n.setParametres(Map.of("nomEtudiant", e.getNomComplet(), "titreOffre", o.getTitre(),
                        "dateLimite", o.getDateLimite().toString()));
                envoyer(n);
                envoyes++;
            }
        }
        return Map.of("joursAvant", joursAvant, "offresConcernees", expirant.size(), "rappelsEnvoyes", envoyes);
    }

    private interface AppelSoap<T> {
        T executer() throws ServiceException;
    }

    private static <T> T appel(AppelSoap<T> a) {
        try {
            return a.executer();
        } catch (ServiceException e) {
            throw new com.universite.stages.rest.commun.ApiException(502, "ERREUR_SERVICE_AMONT",
                    e.getFaultInfo().getMessage(), e.getFaultInfo().getDetails());
        }
    }
}
