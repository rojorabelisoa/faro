package com.faro.client;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Limite du nombre de requêtes sur l'espace client public (désactivable : faro.public.limite.active=false). */
@Configuration
public class ConfigurationEspaceClient {

    @Bean
    @ConditionalOnProperty(name = "faro.public.limite.active", havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<LimiteRequetesPubliques> limiteRequetesPubliques(
            @Value("${faro.public.limite.requetes-par-minute:60}") int requetesParMinute,
            @Value("${faro.public.limite.proxys-de-confiance:0}") int proxysDeConfiance) {
        FilterRegistrationBean<LimiteRequetesPubliques> enregistrement = new FilterRegistrationBean<>(
            new LimiteRequetesPubliques(requetesParMinute, proxysDeConfiance, Clock.systemUTC()));
        enregistrement.addUrlPatterns("/api/public/*");
        // Avant Spring Security et le reste : une requête refusée ne coûte presque rien.
        enregistrement.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return enregistrement;
    }
}
