package com.mockhub.paymentcredential.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.paymentcredential.entity.PaymentCredential;

import jakarta.persistence.LockModeType;

public interface PaymentCredentialRepository extends JpaRepository<PaymentCredential, Long> {

    Optional<PaymentCredential> findByCredentialId(String credentialId);

    List<PaymentCredential> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentCredential p WHERE p.credentialId = :credentialId")
    Optional<PaymentCredential> findByCredentialIdForUpdate(@Param("credentialId") String credentialId);
}
