package com.faro.affaire;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.organisation.Organisation;
import com.faro.user.AppUserService;

@RestController
@RequestMapping("/api/affaires")
public class AffaireController {

    private final AffaireService affaireService;
    private final AppUserService userService;

    public AffaireController(AffaireService affaireService, AppUserService userService) {
        this.affaireService = affaireService;
        this.userService = userService;
    }

    @GetMapping
    public List<AffaireResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return affaireService.list(organisation(jwt)).stream().map(AffaireResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AffaireResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return AffaireResponse.from(affaireService.get(organisation(jwt), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AffaireResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AffaireRequest request) {
        return AffaireResponse.from(affaireService.create(organisation(jwt), request));
    }

    @PutMapping("/{id}")
    public AffaireResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody AffaireRequest request) {
        return AffaireResponse.from(affaireService.update(organisation(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        affaireService.delete(organisation(jwt), id);
    }

    private Organisation organisation(Jwt jwt) {
        return userService.current(jwt).getOrganisation();
    }
}
