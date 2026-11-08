package com.universite.stages.rest.candidatures;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Publication d'événements métier du service Candidatures (patron
 * observateur). L'orchestrateur s'y abonne pour interrompre le processus
 * BPMN d'une candidature annulée par l'étudiant.
 */
public final class CandidatureEvenements {

    private static final List<Consumer<Candidature>> ANNULATION = new CopyOnWriteArrayList<>();

    private CandidatureEvenements() {
    }

    public static void surAnnulation(Consumer<Candidature> abonne) {
        ANNULATION.add(abonne);
    }

    static void publierAnnulation(Candidature c) {
        ANNULATION.forEach(a -> a.accept(c));
    }
}
