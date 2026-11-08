package com.universite.stages.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Accès JDBC à la base relationnelle (H2 ou PostgreSQL).
 * Chaque service ne manipule que ses propres tables : le partage de données
 * entre services passe exclusivement par leurs interfaces SOAP/REST.
 */
public final class Database {

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("db.url"),
                AppConfig.get("db.user"),
                AppConfig.get("db.password"));
    }

    /** Crée le schéma s'il n'existe pas (script schema.sql). */
    public static void init() {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql");
             Connection c = getConnection();
             Statement st = c.createStatement()) {
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String sql : script.split(";")) {
                String trimmed = sql.replaceAll("(?m)^--.*$", "").trim();
                if (!trimmed.isEmpty()) {
                    st.execute(trimmed);
                }
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Initialisation de la base impossible", e);
        }
    }

    /** Exception non vérifiée pour encapsuler les erreurs SQL dans les DAO. */
    public static class DataAccessException extends RuntimeException {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
