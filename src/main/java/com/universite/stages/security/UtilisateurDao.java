package com.universite.stages.security;

import com.universite.stages.common.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Référentiel des comptes utilisateurs, avec cache des authentifications réussies. */
public final class UtilisateurDao {

    /** Cache (identifiant:mot de passe haché en SHA-256 -> utilisateur) : PBKDF2 est volontairement coûteux. */
    private static final Map<String, Utilisateur> CACHE = new ConcurrentHashMap<>();

    private UtilisateurDao() {
    }

    public static Optional<Utilisateur> authentifier(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        String cle = username + ":" + sha256(password);
        Utilisateur cached = CACHE.get(cle);
        if (cached != null) {
            return Optional.of(cached);
        }
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT username, password_hash, role, ref_id FROM utilisateur WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordHasher.verify(password, rs.getString("password_hash"))) {
                    Utilisateur u = new Utilisateur(rs.getString("username"), rs.getString("role"), rs.getString("ref_id"));
                    CACHE.put(cle, u);
                    return Optional.of(u);
                }
            }
        } catch (SQLException e) {
            throw new Database.DataAccessException("Erreur d'authentification", e);
        }
        return Optional.empty();
    }

    /** Authentification d'une application cliente par sa clé d'API (en-tête X-API-Key). */
    public static Optional<Utilisateur> authentifierParCle(String cle) {
        if (cle == null || cle.isBlank()) {
            return Optional.empty();
        }
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT username, role, ref_id FROM utilisateur WHERE api_key_hash = ?")) {
            ps.setString(1, sha256(cle.trim()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Utilisateur(rs.getString("username"), rs.getString("role"), rs.getString("ref_id")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new Database.DataAccessException("Erreur d'authentification", e);
        }
    }

    /** Associe une clé d'API à un compte ; seule son empreinte SHA-256 est conservée. */
    public static void definirCle(String username, String cle) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE utilisateur SET api_key_hash = ? WHERE username = ?")) {
            ps.setString(1, sha256(cle));
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Database.DataAccessException("Enregistrement de la clé impossible", e);
        }
    }

    /** Génère une nouvelle clé aléatoire (256 bits), l'associe au compte et la renvoie une seule fois. */
    public static String genererCle(String username) {
        byte[] b = new byte[32];
        new java.security.SecureRandom().nextBytes(b);
        String cle = "stg_" + java.util.HexFormat.of().formatHex(b);
        definirCle(username, cle);
        return cle;
    }

    public static void creer(String username, String password, String role, String refId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO utilisateur (username, password_hash, role, ref_id) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, PasswordHasher.hash(password));
            ps.setString(3, role);
            ps.setString(4, refId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Database.DataAccessException("Création du compte impossible", e);
        }
    }

    public static void supprimerParRef(String role, String refId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM utilisateur WHERE role = ? AND ref_id = ?")) {
            ps.setString(1, role);
            ps.setString(2, refId);
            ps.executeUpdate();
            CACHE.values().removeIf(u -> role.equals(u.role()) && refId.equals(u.refId()));
        } catch (SQLException e) {
            throw new Database.DataAccessException("Suppression du compte impossible", e);
        }
    }

    public static boolean existe(String username) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM utilisateur WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new Database.DataAccessException("Lecture du compte impossible", e);
        }
    }

    public static boolean aucunUtilisateur() {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM utilisateur");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) == 0;
        } catch (SQLException e) {
            throw new Database.DataAccessException("Lecture des comptes impossible", e);
        }
    }

    private static String sha256(String s) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(d);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
