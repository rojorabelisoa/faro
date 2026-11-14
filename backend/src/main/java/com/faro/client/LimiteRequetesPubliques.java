package com.faro.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite simple du nombre de requêtes sur l'espace client public (/api/public/**) par adresse IP :
 * fenêtre fixe d'une minute, en mémoire (propre à chaque instance du backend, remise à zéro au redémarrage).
 * Au-delà : 429 avec l'en-tête Retry-After.
 *
 * Adresse du client : l'adresse de la connexion, sauf si elle vient d'un réseau privé (proxy faro) et que
 * {@code proxysDeConfiance} > 0. On prend alors dans X-Forwarded-For l'entrée ajoutée par le proxy le plus
 * extérieur : la N-ième en partant de la droite (chaque proxy ajoute à droite l'adresse qu'il voit ; ce que le
 * client a mis lui-même reste à gauche et est ignoré). Production : Caddy puis nginx → 2 ; Docker « complet »
 * sans Caddy : nginx seul → 1 ; backend appelé directement : 0 (X-Forwarded-For ignoré).
 */
public class LimiteRequetesPubliques extends OncePerRequestFilter {

    static final long FENETRE_MS = 60_000;
    private static final int NETTOYAGE_AU_DELA = 10_000;

    private final int requetesParMinute;
    private final int proxysDeConfiance;
    private final Clock horloge;
    private final Map<String, Compteur> compteurs = new ConcurrentHashMap<>();

    private record Compteur(long debut, int nombre) {
    }

    public LimiteRequetesPubliques(int requetesParMinute, int proxysDeConfiance, Clock horloge) {
        this.requetesParMinute = requetesParMinute;
        this.proxysDeConfiance = proxysDeConfiance;
        this.horloge = horloge;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long maintenant = horloge.millis();
        if (compteurs.size() > NETTOYAGE_AU_DELA) {
            compteurs.values().removeIf(c -> maintenant - c.debut() >= FENETRE_MS);
        }
        Compteur compteur = compteurs.compute(adresseClient(request), (ip, c) ->
            c == null || maintenant - c.debut() >= FENETRE_MS ? new Compteur(maintenant, 1)
                : new Compteur(c.debut(), c.nombre() + 1));
        if (compteur.nombre() > requetesParMinute) {
            long attente = Math.max(1, (compteur.debut() + FENETRE_MS - maintenant + 999) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(attente));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Trop de requêtes : réessayez dans une minute."}""");
            return;
        }
        chain.doFilter(request, response);
    }

    String adresseClient(HttpServletRequest request) {
        String distante = request.getRemoteAddr();
        String transmis = request.getHeader("X-Forwarded-For");
        if (proxysDeConfiance <= 0 || transmis == null || transmis.isBlank() || !reseauPrive(distante)) {
            return distante;
        }
        String[] entrees = transmis.split(",");
        String adresse = entrees[Math.max(0, entrees.length - proxysDeConfiance)].trim();
        return adresse.isEmpty() ? distante : adresse;
    }

    /** Boucle locale ou réseau privé (réseau Docker, réseau local) : seul un proxy faro peut s'y trouver. */
    static boolean reseauPrive(String adresse) {
        // Adresse IP littérale seulement (Tomcat ne résout pas les noms) : pas de résolution DNS.
        if (adresse == null || !adresse.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            InetAddress ip = InetAddress.getByName(adresse);
            return ip.isLoopbackAddress() || ip.isSiteLocalAddress() || ip.isLinkLocalAddress()
                || (ip.getAddress().length == 16 && (ip.getAddress()[0] & 0xFE) == 0xFC);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
