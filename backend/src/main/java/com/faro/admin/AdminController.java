package com.faro.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.common.NotFoundException;
import com.faro.organisation.OrganisationRepository;
import com.faro.organisation.Plan;

/**
 * Administration du SaaS (rôle Keycloak « admin », voir SecurityConfig) : les entreprises clientes et leur offre,
 * changée à la main en attendant le paiement en ligne.
 */
@RestController
public class AdminController {

    private final JdbcTemplate jdbc;
    private final OrganisationRepository organisations;

    public AdminController(JdbcTemplate jdbc, OrganisationRepository organisations) {
        this.jdbc = jdbc;
        this.organisations = organisations;
    }

    public record OrganisationAdmin(UUID id, String nom, Plan plan, Instant essaiFin, Instant createdAt,
            int utilisateurs, int affaires, Instant derniereActivite) {
    }

    @GetMapping("/api/admin/organisations")
    public List<OrganisationAdmin> organisations() {
        return jdbc.query("""
            SELECT o.id, o.nom, o.plan, o.essai_fin, o.created_at,
                   (SELECT count(*) FROM app_user u WHERE u.organisation_id = o.id) AS utilisateurs,
                   (SELECT count(*) FROM affaire a WHERE a.organisation_id = o.id) AS affaires,
                   (SELECT max(u.last_login_at) FROM app_user u WHERE u.organisation_id = o.id) AS derniere_activite
            FROM organisation o
            ORDER BY o.created_at DESC
            """, (rs, i) -> new OrganisationAdmin(rs.getObject("id", UUID.class), rs.getString("nom"),
                Plan.valueOf(rs.getString("plan")), instant(rs.getTimestamp("essai_fin")),
                instant(rs.getTimestamp("created_at")), rs.getInt("utilisateurs"), rs.getInt("affaires"),
                instant(rs.getTimestamp("derniere_activite"))));
    }

    public record PlanRequest(@NotNull Plan plan) {
    }

    @PutMapping("/api/admin/organisations/{id}/plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void changerPlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        organisations.findById(id).orElseThrow(() -> new NotFoundException("Entreprise introuvable"))
            .setPlan(request.plan());
    }

    private static Instant instant(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
