package com.faro.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import com.faro.DatabaseCleaner;

@SpringBootTest
class AppUserServiceConcurrencyTest {

    @Autowired
    private AppUserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void simultaneousFirstRequestsCreateASingleUserAndOrganisation() throws Exception {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("premiere-connexion").build();
        int threads = 8;
        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<AppUser>> resultats = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<AppUser> appel = () -> {
                depart.await();
                return userService.current(jwt);
            };
            resultats.add(executor.submit(appel));
        }
        depart.countDown();
        for (Future<AppUser> resultat : resultats) {
            resultat.get();
        }
        executor.shutdown();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organisation", Integer.class)).isEqualTo(1);
    }
}
