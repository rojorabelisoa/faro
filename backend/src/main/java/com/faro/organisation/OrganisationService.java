package com.faro.organisation;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganisationService {

    private final OrganisationRepository repository;
    private final ApplicationEventPublisher events;

    public OrganisationService(OrganisationRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public Organisation create(String nom) {
        Organisation organisation = repository.save(new Organisation(nom));
        events.publishEvent(new OrganisationCreatedEvent(organisation));
        return organisation;
    }
}
