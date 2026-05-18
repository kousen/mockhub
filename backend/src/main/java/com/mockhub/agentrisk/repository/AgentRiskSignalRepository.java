package com.mockhub.agentrisk.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.agentrisk.entity.AgentRiskSignal;
import com.mockhub.agentrisk.entity.AgentRiskSignalType;
import com.mockhub.eval.dto.EvalSeverity;

public interface AgentRiskSignalRepository extends JpaRepository<AgentRiskSignal, Long> {

    @Query("""
            SELECT s FROM AgentRiskSignal s
            WHERE s.userEmail = :userEmail
              AND s.agentId = :agentId
              AND s.createdAt >= :since
            ORDER BY s.createdAt DESC
            """)
    List<AgentRiskSignal> findRecent(@Param("userEmail") String userEmail,
                                     @Param("agentId") String agentId,
                                     @Param("since") Instant since,
                                     Pageable pageable);

    long countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
            String userEmail, String agentId, AgentRiskSignalType signalType, Instant since);

    long countByUserEmailAndAgentIdAndSeverityAndCreatedAtAfter(
            String userEmail, String agentId, EvalSeverity severity, Instant since);

    long countByUserEmailAndAgentIdAndCreatedAtAfter(String userEmail, String agentId, Instant since);
}
