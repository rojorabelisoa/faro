package com.faro.lot;

import static com.faro.TestTokens.user;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.faro.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class LotApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void aNewOrganisationGetsTheDefaultLots() throws Exception {
        mockMvc.perform(get("/api/lots").with(user("lots-defaut")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(LotService.LOTS_PAR_DEFAUT.size())))
            .andExpect(jsonPath("$[0].code").value("01"))
            .andExpect(jsonPath("$[6].nom").value("Plomberie / Sanitaires"));
    }

    @Test
    void createUpdateAndDeleteALot() throws Exception {
        String body = mockMvc.perform(post("/api/lots").with(user("lots-crud"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code": "15", "nom": "Désamiantage"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ordre").value(LotService.LOTS_PAR_DEFAUT.size() + 1))
            .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(put("/api/lots/" + id).with(user("lots-crud"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code": "15", "nom": "Désamiantage / Plomb", "ordre": 2}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("Désamiantage / Plomb"))
            .andExpect(jsonPath("$.ordre").value(2));

        mockMvc.perform(delete("/api/lots/" + id).with(user("lots-crud")))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/lots").with(user("lots-crud")))
            .andExpect(jsonPath("$", hasSize(LotService.LOTS_PAR_DEFAUT.size())));
    }

    @Test
    void lotCodesAreUniqueWithinAnOrganisation() throws Exception {
        mockMvc.perform(post("/api/lots").with(user("lots-doublon"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code": "07", "nom": "Plomberie bis"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void anotherOrganisationCannotChangeTheLot() throws Exception {
        String body = mockMvc.perform(get("/api/lots").with(user("lots-a")))
            .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$[0].id");

        mockMvc.perform(delete("/api/lots/" + id).with(user("lots-b")))
            .andExpect(status().isNotFound());
    }
}
