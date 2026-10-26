package com.faro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.faro.user.AppUserRepository;

/** Tests d'intégration : nécessitent une base PostgreSQL (voir README). */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void meRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsAndStoresTheConnectedUser() throws Exception {
        mockMvc.perform(get("/api/me").with(jwt()
                .jwt(token -> token
                    .subject("sub-123")
                    .claim("email", "demo@faro.local")
                    .claim("preferred_username", "demo")
                    .claim("roles", List.of("user")))
                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value("sub-123"))
            .andExpect(jsonPath("$.email").value("demo@faro.local"))
            .andExpect(jsonPath("$.displayName").value("demo"))
            .andExpect(jsonPath("$.roles", containsInAnyOrder("user")))
            .andExpect(jsonPath("$.organisation.nom").value("Mon entreprise"));

        assertThat(userRepository.findBySubject("sub-123")).isPresent();

        // Un deuxième appel ne crée pas de doublon.
        mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token.subject("sub-123"))))
            .andExpect(status().isOk());
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void adminRoutesRequireTheAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/anything")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsTheFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/me")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk());
    }
}
