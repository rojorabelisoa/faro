package com.faro.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Limite des requêtes sur l'espace client public, activée ici avec 3 requêtes par minute et 2 proxys. */
@SpringBootTest(properties = {
    "faro.public.limite.active=true",
    "faro.public.limite.requetes-par-minute=3",
    "faro.public.limite.proxys-de-confiance=2",
})
@AutoConfigureMockMvc
class LimiteRequetesPubliquesTest {

    @Autowired
    private MockMvc mockMvc;

    /** Requête arrivant par nginx (réseau Docker) : X-Forwarded-For = « ce que dit le client, client, Caddy ». */
    private static RequestPostProcessor viaProxys(String xForwardedFor) {
        return requete -> {
            requete.setRemoteAddr("172.18.0.5");
            requete.addHeader("X-Forwarded-For", xForwardedFor);
            return requete;
        };
    }

    @Test
    void aClientIsLimitedPerMinuteWhateverItPutsInXForwardedFor() throws Exception {
        for (int i = 0; i < 3; i++) {
            // Le client change la partie gauche de l'en-tête à chaque fois : ignorée.
            mockMvc.perform(get("/api/public/client/inconnu").with(viaProxys("10.0.0." + i + ", 203.0.113.7, 172.18.0.2")))
                .andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/public/client/inconnu").with(viaProxys("1.2.3.4, 203.0.113.7, 172.18.0.2")))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.detail").value("Trop de requêtes : réessayez dans une minute."));

        // Un autre client passe ; l'API connectée n'est pas concernée.
        mockMvc.perform(get("/api/public/client/inconnu").with(viaProxys("198.51.100.9, 172.18.0.2")))
            .andExpect(status().isNotFound());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/health").with(viaProxys("203.0.113.7, 172.18.0.2"))).andExpect(status().isOk());
            mockMvc.perform(get("/api/affaires").with(viaProxys("203.0.113.7, 172.18.0.2")))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void theClientAddressIsTakenCautiously() {
        LimiteRequetesPubliques filtre = new LimiteRequetesPubliques(60, 2, Clock.systemUTC());
        assertThat(filtre.adresseClient(requete("172.18.0.5", "203.0.113.7, 172.18.0.2"))).isEqualTo("203.0.113.7");
        assertThat(filtre.adresseClient(requete("172.18.0.5", "6.6.6.6, 203.0.113.7, 172.18.0.2"))).isEqualTo("203.0.113.7");
        // Chaîne plus courte que prévu : l'entrée la plus à gauche.
        assertThat(filtre.adresseClient(requete("172.18.0.5", "203.0.113.7"))).isEqualTo("203.0.113.7");
        assertThat(filtre.adresseClient(requete("172.18.0.5", null))).isEqualTo("172.18.0.5");
        // Connexion directe depuis Internet : l'en-tête n'est pas cru.
        assertThat(filtre.adresseClient(requete("198.51.100.1", "6.6.6.6, 7.7.7.7"))).isEqualTo("198.51.100.1");
        // Sans proxy de confiance : jamais d'en-tête.
        assertThat(new LimiteRequetesPubliques(60, 0, Clock.systemUTC())
            .adresseClient(requete("127.0.0.1", "6.6.6.6"))).isEqualTo("127.0.0.1");
        assertThat(new LimiteRequetesPubliques(60, 1, Clock.systemUTC())
            .adresseClient(requete("127.0.0.1", "6.6.6.6, 203.0.113.7"))).isEqualTo("203.0.113.7");
        assertThat(LimiteRequetesPubliques.reseauPrive("::1")).isTrue();
        assertThat(LimiteRequetesPubliques.reseauPrive("fd00::1")).isTrue();
        assertThat(LimiteRequetesPubliques.reseauPrive("2001:db8::1")).isFalse();
        assertThat(LimiteRequetesPubliques.reseauPrive("exemple.fr")).isFalse();
    }

    @Test
    void theCounterRestartsAfterAMinute() throws Exception {
        MutableClock horloge = new MutableClock();
        LimiteRequetesPubliques filtre = new LimiteRequetesPubliques(2, 0, horloge);
        assertThat(statut(filtre)).isEqualTo(200);
        assertThat(statut(filtre)).isEqualTo(200);
        assertThat(statut(filtre)).isEqualTo(429);
        horloge.instant = horloge.instant.plusSeconds(61);
        assertThat(statut(filtre)).isEqualTo(200);
    }

    private static MockHttpServletRequest requete(String distante, String xForwardedFor) {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/api/public/client/x");
        requete.setRemoteAddr(distante);
        if (xForwardedFor != null) {
            requete.addHeader("X-Forwarded-For", xForwardedFor);
        }
        return requete;
    }

    private static int statut(LimiteRequetesPubliques filtre) throws Exception {
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filtre.doFilter(requete("203.0.113.7", null), reponse, new MockFilterChain());
        return reponse.getStatus();
    }

    private static final class MutableClock extends Clock {
        Instant instant = Instant.parse("2026-11-14T10:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
