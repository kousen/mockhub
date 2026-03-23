package com.mockhub.mandate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.mandate.entity.Mandate;

public interface MandateRepository extends JpaRepository<Mandate, Long> {

    List<Mandate> findByAgentIdAndUserEmailAndStatus(String agentId, String userEmail, String status);

    List<Mandate> findByUserEmailAndStatus(String userEmail, String status);

    Optional<Mandate> findByMandateId(String mandateId);
}
