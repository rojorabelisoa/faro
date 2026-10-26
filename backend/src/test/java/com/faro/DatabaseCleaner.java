package com.faro;

import org.springframework.jdbc.core.JdbcTemplate;

/** Vide les tables métier entre deux tests (la base de test est partagée). */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void clean(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE piece, lot, affaire, app_user, organisation CASCADE");
    }
}
