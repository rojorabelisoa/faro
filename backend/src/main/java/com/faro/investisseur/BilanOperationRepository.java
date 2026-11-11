package com.faro.investisseur;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BilanOperationRepository extends JpaRepository<BilanOperation, UUID> {
}
