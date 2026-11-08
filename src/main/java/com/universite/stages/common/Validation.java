package com.universite.stages.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validation déclarative des données entrantes. Les erreurs sont accumulées
 * puis levées ensemble afin que le client reçoive un diagnostic complet.
 */
public final class Validation {

    public static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    public static final Pattern TELEPHONE = Pattern.compile("^\\+?[0-9 .-]{8,20}$");
    public static final Pattern MATRICULE = Pattern.compile("^[A-Z]{1,3}[0-9]{4,10}$");

    private final List<String> erreurs = new ArrayList<>();

    public static Validation create() {
        return new Validation();
    }

    public Validation obligatoire(String champ, Object valeur) {
        if (valeur == null || (valeur instanceof String s && s.isBlank())) {
            erreurs.add("Le champ '" + champ + "' est obligatoire");
        }
        return this;
    }

    public Validation longueurMax(String champ, String valeur, int max) {
        if (valeur != null && valeur.length() > max) {
            erreurs.add("Le champ '" + champ + "' dépasse " + max + " caractères");
        }
        return this;
    }

    public Validation format(String champ, String valeur, Pattern pattern, String attendu) {
        if (valeur != null && !valeur.isBlank() && !pattern.matcher(valeur.trim()).matches()) {
            erreurs.add("Le champ '" + champ + "' est invalide (attendu : " + attendu + ")");
        }
        return this;
    }

    public Validation email(String champ, String valeur) {
        return format(champ, valeur, EMAIL, "adresse e-mail");
    }

    public Validation telephone(String champ, String valeur) {
        return format(champ, valeur, TELEPHONE, "numéro de téléphone");
    }

    public Validation positif(String champ, Integer valeur) {
        if (valeur != null && valeur <= 0) {
            erreurs.add("Le champ '" + champ + "' doit être strictement positif");
        }
        return this;
    }

    public Validation entre(String champ, Number valeur, double min, double max) {
        if (valeur != null && (valeur.doubleValue() < min || valeur.doubleValue() > max)) {
            erreurs.add("Le champ '" + champ + "' doit être compris entre " + min + " et " + max);
        }
        return this;
    }

    public Validation dansListe(String champ, String valeur, List<String> valeurs) {
        if (valeur != null && !valeurs.contains(valeur)) {
            erreurs.add("Le champ '" + champ + "' doit valoir l'une des valeurs " + valeurs);
        }
        return this;
    }

    public Validation dateFuture(String champ, LocalDate date) {
        if (date != null && date.isBefore(LocalDate.now())) {
            erreurs.add("Le champ '" + champ + "' doit être une date future");
        }
        return this;
    }

    public Validation regle(boolean condition, String message) {
        if (!condition) {
            erreurs.add(message);
        }
        return this;
    }

    public boolean estValide() {
        return erreurs.isEmpty();
    }

    public List<String> erreurs() {
        return erreurs;
    }

    public String message() {
        return String.join(" ; ", erreurs);
    }
}
