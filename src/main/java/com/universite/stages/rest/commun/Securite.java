package com.universite.stages.rest.commun;

import com.universite.stages.security.Role;
import com.universite.stages.security.Utilisateur;
import jakarta.ws.rs.core.SecurityContext;

/** Règles d'accès aux données propres à chaque acteur (étudiant, entreprise). */
public final class Securite {

    private Securite() {
    }

    public static Utilisateur utilisateur(SecurityContext sc) {
        return (Utilisateur) sc.getUserPrincipal();
    }

    public static void exigerEtudiant(SecurityContext sc, String matricule) {
        Utilisateur u = utilisateur(sc);
        if (u.aRole(Role.ETUDIANT) && !u.refId().equals(matricule)) {
            throw ApiException.interdit("Accès limité à vos propres données");
        }
        if (u.aRole(Role.ENTREPRISE)) {
            throw ApiException.interdit("Accès réservé à l'étudiant concerné");
        }
    }

    /** Vérifie qu'un étudiant / une entreprise accède bien à une ressource qui le concerne. */
    public static void exigerProprietaire(SecurityContext sc, String matricule, Long entrepriseId) {
        Utilisateur u = utilisateur(sc);
        if (u.aRole(Role.ETUDIANT) && !u.refId().equals(matricule)) {
            throw ApiException.interdit("Accès limité à vos propres données");
        }
        if (u.aRole(Role.ENTREPRISE) && (entrepriseId == null || !u.refId().equals(String.valueOf(entrepriseId)))) {
            throw ApiException.interdit("Accès limité aux données de votre entreprise");
        }
    }
}
