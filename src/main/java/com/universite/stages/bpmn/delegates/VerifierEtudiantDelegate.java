package com.universite.stages.bpmn.delegates;

import com.universite.stages.clients.SoapClients;
import com.universite.stages.soap.etudiants.Etudiant;
import com.universite.stages.soap.etudiants.VerificationEtudiant;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 2 — Vérification de l'étudiant (service SOAP Étudiants). */
public class VerifierEtudiantDelegate extends AbstractServiceDelegate {

    @Override
    protected void executer(DelegateExecution ex) {
        String matricule = str(ex, "matricule");
        VerificationEtudiant v = soap("etudiants", () -> SoapClients.etudiants().verifierEtudiant(matricule));
        ex.setVariable("etudiantExiste", v.isExiste());
        ex.setVariable("etudiantEligible", v.isEligible());
        if (v.isExiste()) {
            Etudiant e = soap("etudiants", () -> SoapClients.etudiants().consulterEtudiant(matricule));
            ex.setVariable("nomEtudiant", e.getNomComplet());
            ex.setVariable("emailEtudiant", e.getEmail());
            ex.setVariable("filiere", e.getFiliere());
            ex.setVariable("niveau", e.getNiveau());
            ex.setVariable("competences", e.getCompetences());
        } else {
            ex.setVariable("nomEtudiant", matricule);
        }
        if (!v.isEligible()) {
            ex.setVariable("recevable", false);
            ex.setVariable("motifRejet", v.getMessage());
        }
    }
}
