package com.universite.stages.common;

/**
 * Masquage des données personnelles exposées à des tiers (entreprises) et
 * dans les journaux : seuls quelques caractères restent lisibles.
 */
public final class Masquage {

    private Masquage() {
    }

    /** {@code +212 6 12 34 56 01} devient {@code +212 6 •• •• •• 01}. */
    public static String telephone(String tel) {
        if (tel == null || tel.isBlank()) {
            return tel;
        }
        String t = tel.trim();
        int debut = Math.min(7, Math.max(0, t.length() - 2));
        StringBuilder sb = new StringBuilder(t.substring(0, debut));
        for (int i = debut; i < t.length() - 2; i++) {
            sb.append(Character.isDigit(t.charAt(i)) ? '•' : t.charAt(i));
        }
        return sb.append(t.substring(t.length() - 2)).toString();
    }

    /** {@code yasmine.benali@etu.example} devient {@code y•••••@etu.example}. */
    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "•••••" + email.substring(at);
    }
}
