package com.faro.equipe;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.equipe.EquipeDtos.EntrepriseResponse;
import com.faro.equipe.EquipeDtos.InvitationInfo;
import com.faro.equipe.EquipeDtos.InvitationRequest;
import com.faro.equipe.EquipeDtos.InvitationResponse;
import com.faro.equipe.EquipeDtos.RenommerRequest;
import com.faro.equipe.EquipeDtos.RoleRequest;
import com.faro.user.AppUserService;

@RestController
public class EquipeController {

    private final EquipeService equipeService;
    private final AppUserService userService;

    public EquipeController(EquipeService equipeService, AppUserService userService) {
        this.equipeService = equipeService;
        this.userService = userService;
    }

    @GetMapping("/api/entreprise")
    public EntrepriseResponse entreprise(@AuthenticationPrincipal Jwt jwt) {
        return equipeService.entreprise(userService.current(jwt));
    }

    @PutMapping("/api/entreprise")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void renommer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RenommerRequest request) {
        equipeService.renommer(userService.current(jwt), request.nom());
    }

    @PutMapping("/api/entreprise/membres/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request) {
        equipeService.changerRole(userService.current(jwt), id, request.role());
    }

    @DeleteMapping("/api/entreprise/membres/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        equipeService.retirer(userService.current(jwt), id);
    }

    @GetMapping("/api/entreprise/invitations")
    public List<InvitationResponse> invitations(@AuthenticationPrincipal Jwt jwt) {
        return equipeService.invitations(userService.current(jwt)).stream().map(InvitationResponse::from).toList();
    }

    @PostMapping("/api/entreprise/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse inviter(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody InvitationRequest request) {
        return InvitationResponse.from(equipeService.inviter(userService.current(jwt), request.role(), request.email()));
    }

    @DeleteMapping("/api/entreprise/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoquer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        equipeService.revoquer(userService.current(jwt), id);
    }

    @GetMapping("/api/invitations/{code}")
    public InvitationInfo info(@PathVariable String code) {
        return equipeService.info(code);
    }

    @PostMapping("/api/invitations/{code}/accepter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accepter(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        equipeService.accepter(userService.current(jwt), code);
    }
}
