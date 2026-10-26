package com.faro.organisation;

/** Publié (dans la transaction) à la création d'une organisation, pour initialiser ses données par défaut. */
public record OrganisationCreatedEvent(Organisation organisation) {
}
