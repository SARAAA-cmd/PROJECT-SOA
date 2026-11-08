package com.universite.stages.security;

import java.security.Principal;

/**
 * Utilisateur authentifié. {@code refId} relie le compte à la ressource
 * métier qu'il représente (matricule de l'étudiant, identifiant d'entreprise).
 */
public record Utilisateur(String username, String role, String refId) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public boolean aRole(String... roles) {
        for (String r : roles) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean estInterne() {
        return aRole(Role.RESPONSABLE, Role.SYSTEME);
    }
}
