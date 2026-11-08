package com.universite.stages.bpmn.delegates;

import com.universite.stages.clients.SoapClients;
import com.universite.stages.soap.commun.ServiceException;
import com.universite.stages.soap.entreprises.Entreprise;
import com.universite.stages.soap.entreprises.VerificationEntreprise;
import com.universite.stages.soap.offres.Compatibilite;
import com.universite.stages.soap.offres.Offre;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.ArrayList;
import java.util.List;

/**
 * Étape 3 — Vérification de l'offre : offre active, entreprise référencée
 * et compatibilité avec le profil académique (services SOAP Offres et Entreprises).
 */
public class VerifierOffreDelegate extends AbstractServiceDelegate {

    @Override
    protected void executer(DelegateExecution ex) {
        long offreId = lng(ex, "offreId");
        List<String> motifs = new ArrayList<>();
        Offre o;
        try {
            o = soap("offres", () -> SoapClients.offres().consulterOffre(offreId));
        } catch (FauteMetier f) {
            if (!ServiceException.INTROUVABLE.equals(f.getCode())) {
                throw f;
            }
            ex.setVariable("offreValide", false);
            ex.setVariable("titreOffre", "offre n°" + offreId);
            ex.setVariable("motifRejet", f.getMessage());
            return;
        }
        ex.setVariable("titreOffre", o.getTitre());
        ex.setVariable("entrepriseId", o.getEntrepriseId());
        ex.setVariable("dureeMois", o.getDureeMois());
        if (!Offre.ACTIVE.equals(o.getStatut())) {
            motifs.add("L'offre n'est pas active (statut " + o.getStatut() + ")");
        }

        VerificationEntreprise ve = soap("entreprises",
                () -> SoapClients.entreprises().verifierEntreprise(o.getEntrepriseId()));
        if (ve.isExiste()) {
            Entreprise e = soap("entreprises", () -> SoapClients.entreprises().consulterEntreprise(o.getEntrepriseId()));
            ex.setVariable("entreprise", e.getRaisonSociale());
            ex.setVariable("emailEntreprise", e.getContactEmail());
        }
        if (!ve.isReferencee()) {
            motifs.add(ve.getMessage());
        }

        Compatibilite c = soap("offres", () -> SoapClients.offres().verifierCompatibilite(offreId,
                str(ex, "filiere"), str(ex, "niveau"), str(ex, "competences")));
        ex.setVariable("scoreCompatibilite", c.getScoreCompetences());
        motifs.addAll(c.getMotifs());

        ex.setVariable("offreValide", motifs.isEmpty());
        if (!motifs.isEmpty()) {
            ex.setVariable("motifRejet", String.join(" ; ", motifs));
        }
    }
}
