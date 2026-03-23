package com.mockhub.mandate.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;

@Service
public class MandateService {

    private static final Logger log = LoggerFactory.getLogger(MandateService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final MandateRepository mandateRepository;

    public MandateService(MandateRepository mandateRepository) {
        this.mandateRepository = mandateRepository;
    }

    @Transactional
    public MandateDto createMandate(CreateMandateRequest request) {
        Mandate mandate = new Mandate();
        mandate.setMandateId(UUID.randomUUID().toString());
        mandate.setAgentId(request.agentId());
        mandate.setUserEmail(request.userEmail());
        mandate.setScope(request.scope());
        mandate.setMaxSpendPerTransaction(request.maxSpendPerTransaction());
        mandate.setMaxSpendTotal(request.maxSpendTotal());
        mandate.setTotalSpent(BigDecimal.ZERO);
        mandate.setAllowedCategories(request.allowedCategories());
        mandate.setAllowedEvents(request.allowedEvents());
        mandate.setStatus(STATUS_ACTIVE);
        mandate.setExpiresAt(request.expiresAt());

        Mandate saved = mandateRepository.save(mandate);
        log.info("Created mandate {} for agent '{}' and user '{}'",
                saved.getMandateId(), saved.getAgentId(), saved.getUserEmail());
        return toDto(saved);
    }

    @Transactional
    public void revokeMandate(String mandateId) {
        Mandate mandate = mandateRepository.findByMandateId(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException("Mandate", "mandateId", mandateId));
        mandate.setStatus("REVOKED");
        mandate.setRevokedAt(Instant.now());
        mandateRepository.save(mandate);
        log.info("Revoked mandate {}", mandateId);
    }

    @Transactional(readOnly = true)
    public List<MandateDto> listMandates(String userEmail) {
        List<Mandate> mandates = mandateRepository.findByUserEmailAndStatus(userEmail, STATUS_ACTIVE);
        return mandates.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Mandate getActiveMandate(String agentId, String userEmail) {
        List<Mandate> mandates = mandateRepository.findByAgentIdAndUserEmailAndStatus(
                agentId, userEmail, STATUS_ACTIVE);

        Instant now = Instant.now();
        return mandates.stream()
                .filter(m -> m.getExpiresAt() == null || m.getExpiresAt().isAfter(now))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void recordSpend(String mandateId, BigDecimal amount) {
        Mandate mandate = mandateRepository.findByMandateId(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException("Mandate", "mandateId", mandateId));
        BigDecimal newTotal = mandate.getTotalSpent().add(amount);
        mandate.setTotalSpent(newTotal);
        mandateRepository.save(mandate);
        log.info("Recorded spend of {} on mandate {}, new total: {}", amount, mandateId, newTotal);
    }

    @Transactional(readOnly = true)
    public boolean validateAction(String agentId, String userEmail, String requiredScope,
                                  BigDecimal amount, String categorySlug, String eventSlug) {
        List<Mandate> mandates = mandateRepository.findByAgentIdAndUserEmailAndStatus(
                agentId, userEmail, STATUS_ACTIVE);

        Instant now = Instant.now();
        Mandate mandate = mandates.stream()
                .filter(m -> m.getExpiresAt() == null || m.getExpiresAt().isAfter(now))
                .findFirst()
                .orElse(null);

        if (mandate == null) {
            log.warn("No active mandate for agent '{}' and user '{}'", agentId, userEmail);
            return false;
        }

        return validateMandateConstraints(mandate, requiredScope, amount, categorySlug, eventSlug);
    }

    private boolean validateMandateConstraints(Mandate mandate, String requiredScope,
                                                BigDecimal amount, String categorySlug,
                                                String eventSlug) {
        if (!scopeCovers(mandate.getScope(), requiredScope)) {
            log.warn("Mandate {} scope '{}' does not cover required scope '{}'",
                    mandate.getMandateId(), mandate.getScope(), requiredScope);
            return false;
        }

        if (amount != null && mandate.getMaxSpendPerTransaction() != null
                && amount.compareTo(mandate.getMaxSpendPerTransaction()) > 0) {
            log.warn("Mandate {} transaction amount {} exceeds per-transaction limit {}",
                    mandate.getMandateId(), amount, mandate.getMaxSpendPerTransaction());
            return false;
        }

        if (amount != null && mandate.getMaxSpendTotal() != null
                && mandate.getTotalSpent().add(amount).compareTo(mandate.getMaxSpendTotal()) > 0) {
            log.warn("Mandate {} projected total exceeds total spend limit {}",
                    mandate.getMandateId(), mandate.getMaxSpendTotal());
            return false;
        }

        if (categorySlug != null && mandate.getAllowedCategories() != null
                && !parseCommaSeparated(mandate.getAllowedCategories()).contains(categorySlug)) {
            log.warn("Mandate {} does not allow category '{}'",
                    mandate.getMandateId(), categorySlug);
            return false;
        }

        if (eventSlug != null && mandate.getAllowedEvents() != null
                && !parseCommaSeparated(mandate.getAllowedEvents()).contains(eventSlug)) {
            log.warn("Mandate {} does not allow event '{}'",
                    mandate.getMandateId(), eventSlug);
            return false;
        }

        return true;
    }

    private boolean scopeCovers(String grantedScope, String requiredScope) {
        if ("PURCHASE".equals(grantedScope)) {
            return true;
        }
        return grantedScope.equals(requiredScope);
    }

    private Set<String> parseCommaSeparated(String value) {
        return Arrays.stream(value.split(","))
                .map(String::strip)
                .collect(Collectors.toSet());
    }

    private MandateDto toDto(Mandate mandate) {
        BigDecimal remainingBudget = null;
        if (mandate.getMaxSpendTotal() != null) {
            remainingBudget = mandate.getMaxSpendTotal().subtract(mandate.getTotalSpent());
        }
        return new MandateDto(
                mandate.getId(),
                mandate.getMandateId(),
                mandate.getAgentId(),
                mandate.getUserEmail(),
                mandate.getScope(),
                mandate.getMaxSpendPerTransaction(),
                mandate.getMaxSpendTotal(),
                mandate.getTotalSpent(),
                remainingBudget,
                mandate.getAllowedCategories(),
                mandate.getAllowedEvents(),
                mandate.getStatus(),
                mandate.getExpiresAt(),
                mandate.getCreatedAt()
        );
    }
}
