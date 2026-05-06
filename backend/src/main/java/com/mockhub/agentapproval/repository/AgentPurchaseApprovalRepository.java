package com.mockhub.agentapproval.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.agentapproval.entity.AgentPurchaseApproval;

import jakarta.persistence.LockModeType;

public interface AgentPurchaseApprovalRepository extends JpaRepository<AgentPurchaseApproval, Long> {

    Optional<AgentPurchaseApproval> findByApprovalId(String approvalId);

    List<AgentPurchaseApproval> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentPurchaseApproval a WHERE a.approvalId = :approvalId")
    Optional<AgentPurchaseApproval> findByApprovalIdForUpdate(@Param("approvalId") String approvalId);
}
