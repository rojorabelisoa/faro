package com.faro.demo;

import static com.faro.TestTokens.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.faro.DatabaseCleaner;

@SpringBootTest(properties = "faro.demo.donnees=true")
@AutoConfigureMockMvc
class DonneesDemoTest {

    @Autowired
    private DonneesDemo donneesDemo;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void loadsTheDemoCompanyOnceForTheDemoAccounts() throws Exception {
        DatabaseCleaner.clean(jdbc);
        assertThat(donneesDemo.charger()).isTrue();
        assertThat(donneesDemo.charger()).isFalse();

        mockMvc.perform(get("/api/me").with(user(DonneesDemo.SUBJECT_DEMO)))
            .andExpect(jsonPath("$.organisation.nom").value("Rénov Lyon (démo)"))
            .andExpect(jsonPath("$.roleEntreprise").value("GERANT"));
        mockMvc.perform(get("/api/affaires").with(user(DonneesDemo.SUBJECT_ADMIN)))
            .andExpect(jsonPath("$", hasSize(7)));
        mockMvc.perform(get("/api/ouvrages").with(user(DonneesDemo.SUBJECT_DEMO)))
            .andExpect(jsonPath("$", hasSize(21)));
        mockMvc.perform(get("/api/tableau-de-bord").with(user(DonneesDemo.SUBJECT_DEMO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affairesActives.valeur").value(5))
            .andExpect(jsonPath("$.transformation.gagnees").value(3))
            .andExpect(jsonPath("$.transformation.perdues").value(1))
            .andExpect(jsonPath("$.margeReelle.valeur").isNumber())
            .andExpect(jsonPath("$.enAttenteClient.valeur").value(420.00))
            .andExpect(jsonPath("$.aFaire[*].type").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM avenant WHERE statut = 'VALIDE'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bilan_operation", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ligne_devis WHERE gamme IS NOT NULL", Integer.class)).isEqualTo(3);
    }
}
