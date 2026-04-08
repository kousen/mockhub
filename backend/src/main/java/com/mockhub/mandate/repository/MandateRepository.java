package com.mockhub.mandate.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.mandate.entity.Mandate;

public interface MandateRepository extends JpaRepository<Mandate, Long> {

    List<Mandate> findByAgentIdAndUserEmailAndStatus(String agentId, String userEmail, String status);

    List<Mandate> findByUserEmailAndStatus(String userEmail, String status);

    List<Mandate> findByUserEmail(String userEmail);

    Optional<Mandate> findByMandateId(String mandateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mandate m WHERE m.mandateId = :mandateId")
    Optional<Mandate> findByMandateIdForUpdate(@Param("mandateId") String mandateId);
}
