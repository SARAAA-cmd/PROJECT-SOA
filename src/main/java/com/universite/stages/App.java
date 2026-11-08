package com.universite.stages;

import com.universite.stages.common.AppConfig;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Lancement de la plateforme : {@code java -jar target/gestion-stages.jar}. */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$-7s [%3$s] %5$s%6$s%n");
        Serveur serveur = new Serveur();
        serveur.demarrer();
        Runtime.getRuntime().addShutdownHook(new Thread(serveur::arreter));
        String base = AppConfig.baseUrl();
        System.out.println("""

                ==============================================================
                 Plateforme de gestion des stages démarrée
                --------------------------------------------------------------
                 Interface web        : %1$s/
                 WSDL Étudiants       : %1$s/ws/etudiants?wsdl
                 WSDL Entreprises     : %1$s/ws/entreprises?wsdl
                 WSDL Offres          : %1$s/ws/offres?wsdl
                 API REST             : %1$s/api/{candidatures|notifications|stages|processus}
                ==============================================================
                """.formatted(base));
    }
}
