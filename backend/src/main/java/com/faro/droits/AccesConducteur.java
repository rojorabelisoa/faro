package com.faro.droits;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ouvre un endpoint (méthode ou contrôleur entier) au conducteur de travaux.
 * Par défaut, tout endpoint authentifié lui est refusé (403) : voir {@link DroitsInterceptor}.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AccesConducteur {
}
