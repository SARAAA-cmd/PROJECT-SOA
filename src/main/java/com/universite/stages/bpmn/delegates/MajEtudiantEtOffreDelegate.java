package com.universite.stages.bpmn.delegates;

import com.universite.stages.clients.SoapClients;
import com.universite.stages.soap.etudiants.Etudiant;
import com.universite.stages.soap.offres.Offre;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * Étape 9 (suite) — Mise à jour du statut de l'étudiant (EN_STAGE) et
 * réservation d'une place sur l'offre (services SOAP Étudiants et Offres).
 */
public class MajEtudiantEtOffreDelegate extends AbstractServiceDelegate {

    @Override
    protected void executer(DelegateExecution ex) {
        String matricule = str(ex, "matricule");
        long offreId = lng(ex, "offreId");
        soap("etudiants", () -> SoapClients.etudiants().changerStatutEtudiant(matricule, Etudiant.EN_STAGE));
        ex.setVariable("statutEtudiant", Etudiant.EN_STAGE);
        if (!Boolean.TRUE.equals(ex.getVariable("placeReservee"))) {
            try {
                Offre o = soap("offres", () -> SoapClients.offres().reserverPlace(offreId));
                ex.setVariable("placesRestantes", o.getPlacesDisponibles());
                ex.setVariable("placeReservee", true);
            } catch (FauteMetier f) {
                // plus de place : situation à arbitrer par le responsable, consignée sans bloquer
                ex.setVariable("incidents", str(ex, "incidents") + "\nRéservation de place impossible : " + f.getMessage());
            }
        }
    }
}
