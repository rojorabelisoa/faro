package com.faro.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;
import com.faro.user.AppUserService;

@Configuration
public class CurrentOrganisationResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    private final AppUserService userService;

    public CurrentOrganisationResolver(AppUserService userService) {
        this.userService = userService;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOrganisation.class)
            && parameter.getParameterType().equals(Organisation.class);
    }

    @Override
    public Organisation resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        JwtAuthenticationToken authentication =
            (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return userService.current(authentication.getToken()).getOrganisation();
    }
}
