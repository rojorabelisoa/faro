package com.faro.droits;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Point de contrôle central des rôles : un conducteur de travaux n'accède qu'aux endpoints marqués
 * {@link AccesConducteur} (refus par défaut, un nouvel endpoint lui est donc fermé tant qu'on ne l'ouvre pas).
 * Les endpoints publics (espace client), d'administration SaaS, /api/me et /api/health ne sont pas concernés.
 */
@Configuration
public class DroitsInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final Droits droits;

    public DroitsInterceptor(Droits droits) {
        this.droits = droits;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/public/**", "/api/admin/**", "/api/me", "/api/health");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod methode
                && !methode.hasMethodAnnotation(AccesConducteur.class)
                && !methode.getBeanType().isAnnotationPresent(AccesConducteur.class)
                && droits.estConducteur()) {
            throw Droits.interdit();
        }
        return true;
    }
}
