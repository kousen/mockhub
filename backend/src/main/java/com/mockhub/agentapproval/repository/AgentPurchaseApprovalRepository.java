package com.mockhub.agentapproval.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.agentapproval.entity.AgentPurchaseApproval;
import com.mockhub.agentapproval.entity.AgentPurchaseApprovalStatus;

import jakarta.persistence.LockModeType;

public interface AgentPurchaseApprovalRepository extends JpaRepository<AgentPurchaseApproval, Long> {

    Optional<AgentPurchaseApproval> findByApprovalId(String approvalId);

    Optional<AgentPurchaseApproval> findFirstByFinalOrderNumberOrderByCreatedAtDesc(String finalOrderNumber);

    List<AgentPurchaseApproval> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentPurchaseApproval a WHERE a.approvalId = :approvalId")
    Optional<AgentPurchaseApproval> findByApprovalIdForUpdate(@Param("approvalId") String approvalId);

    @Modifying
    @Query("""
            UPDATE AgentPurchaseApproval a
            SET a.status = :expired
            WHERE a.status = :proposed
              AND a.expiresAt IS NOT NULL
              AND a.expiresAt <= :now
            """)
    int expireProposedApprovals(@Param("now") Instant now,
                                @Param("proposed") AgentPurchaseApprovalStatus proposed,
                                @Param("expired") AgentPurchaseApprovalStatus expired);
}
