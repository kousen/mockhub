package com.mockhub.paymentcredential.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.paymentcredential.entity.PaymentCredential;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;

import jakarta.persistence.LockModeType;

public interface PaymentCredentialRepository extends JpaRepository<PaymentCredential, Long> {

    Optional<PaymentCredential> findByCredentialId(String credentialId);

    Optional<PaymentCredential> findFirstByConsumedByOrderNumberOrderByConsumedAtDesc(String consumedByOrderNumber);

    List<PaymentCredential> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentCredential p WHERE p.credentialId = :credentialId")
    Optional<PaymentCredential> findByCredentialIdForUpdate(@Param("credentialId") String credentialId);

    @Modifying
    @Query("""
            UPDATE PaymentCredential p
            SET p.status = :expired
            WHERE p.status = :active
              AND p.expiresAt IS NOT NULL
              AND p.expiresAt <= :now
            """)
    int expireActiveCredentials(@Param("now") Instant now,
                                @Param("active") PaymentCredentialStatus active,
                                @Param("expired") PaymentCredentialStatus expired);
}
