package com.universite.stages.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration centralisée : application.properties, surchargée par les
 * propriétés système (-Dserver.port=...) puis les variables d'environnement
 * (SERVER_PORT=...).
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger application.properties", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null) {
            return sys;
        }
        String env = System.getenv(key.toUpperCase().replace('.', '_').replace('-', '_'));
        if (env != null) {
            return env;
        }
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String v = get(key);
        return v == null ? defaultValue : v;
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key);
        return v == null ? defaultValue : Integer.parseInt(v.trim());
    }

    public static void set(String key, String value) {
        System.setProperty(key, value);
    }

    public static String baseUrl() {
        return get("server.base-url", "http://localhost:" + getInt("server.port", 8090));
    }
}
