package com.universite.stages;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/** Sert l'interface web (HTML/CSS/JS) depuis le classpath. */
class FichiersStatiques implements HttpHandler {

    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "json", "application/json",
            "woff", "font/woff",
            "woff2", "font/woff2");

    private final String racine;

    FichiersStatiques(String racine) {
        this.racine = racine;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String chemin = ex.getRequestURI().getPath();
        if (chemin.equals("/") || chemin.isEmpty()) {
            chemin = "/index.html";
        }
        if (chemin.contains("..")) {
            ex.sendResponseHeaders(400, -1);
            return;
        }
        try (InputStream in = getClass().getResourceAsStream(racine + chemin)) {
            if (in == null) {
                byte[] msg = "404 - ressource introuvable".getBytes();
                ex.sendResponseHeaders(404, msg.length);
                ex.getResponseBody().write(msg);
                ex.close();
                return;
            }
            byte[] contenu = in.readAllBytes();
            String ext = chemin.substring(chemin.lastIndexOf('.') + 1);
            ex.getResponseHeaders().add("Content-Type", TYPES.getOrDefault(ext, "application/octet-stream"));
            ex.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
            ex.getResponseHeaders().add("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, contenu.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(contenu);
            }
        }
    }
}
