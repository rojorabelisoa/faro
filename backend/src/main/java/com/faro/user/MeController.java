package com.faro.user;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AppUserService userService;

    public MeController(AppUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt, JwtAuthenticationToken authentication) {
        AppUser user = userService.syncFromToken(jwt);
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .toList();
        return new MeResponse(user.getId(), user.getSubject(), user.getEmail(), user.getDisplayName(), roles,
            new OrganisationResponse(user.getOrganisation().getId(), user.getOrganisation().getNom()));
    }

    public record MeResponse(UUID id, String subject, String email, String displayName, List<String> roles,
            OrganisationResponse organisation) {
    }

    public record OrganisationResponse(UUID id, String nom) {
    }
}
