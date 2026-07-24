package com.mockhub.mandate.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.event.entity.Category;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;

@Service
public class MandateService {

    private static final Logger log = LoggerFactory.getLogger(MandateService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String MANDATE_RESOURCE = "Mandate";
    private static final String MANDATE_ID_FIELD = "mandateId";
    private static final String APPROVAL_MODE_AUTO_PURCHASE = "AUTO_PURCHASE";
    private static final String APPROVAL_MODE_APPROVAL_REQUIRED = "APPROVAL_REQUIRED";

    private final MandateRepository mandateRepository;
    private final CategoryRepository categoryRepository;

    public MandateService(MandateRepository mandateRepository, CategoryRepository categoryRepository) {
        this.mandateRepository = mandateRepository;
        this.categoryRepository = categoryRepository;
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
        mandate.setAllowedCategories(normalizeAndValidateCategories(request.allowedCategories()));
        mandate.setAllowedEvents(request.allowedEvents());
        mandate.setAllowedSections(request.allowedSections());
        mandate.setApprovalMode(normalizeApprovalMode(request.approvalMode()));
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
                .orElseThrow(() -> new ResourceNotFoundException(MANDATE_RESOURCE, MANDATE_ID_FIELD, mandateId));
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
    public List<MandateDto> listAllMandates(String userEmail) {
        List<Mandate> mandates = mandateRepository.findByUserEmail(userEmail);
        return mandates.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void revokeMandate(String mandateId, String userEmail) {
        Mandate mandate = mandateRepository.findByMandateId(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(MANDATE_RESOURCE, MANDATE_ID_FIELD, mandateId));
        if (!mandate.getUserEmail().equals(userEmail)) {
            throw new UnauthorizedException("You do not own this mandate");
        }
        mandate.setStatus("REVOKED");
        mandate.setRevokedAt(Instant.now());
        mandateRepository.save(mandate);
        log.info("Revoked mandate {} by user {}", mandateId, userEmail);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("java:S6809") // Self-invocation is intentional — simple delegation to the full overload
    public Mandate getActiveMandate(String agentId, String userEmail) {
        return getActiveMandate(agentId, userEmail, null);
    }

    @Transactional(readOnly = true)
    public Mandate getActiveMandate(String agentId, String userEmail, String mandateId) {
        List<Mandate> mandates = mandateRepository.findByAgentIdAndUserEmailAndStatus(
                agentId, userEmail, STATUS_ACTIVE);

        Instant now = Instant.now();
        return mandates.stream()
                .filter(m -> mandateId == null || mandateId.isBlank() || mandateId.equals(m.getMandateId()))
                .filter(m -> m.getExpiresAt() == null || m.getExpiresAt().isAfter(now))
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<MandateDto> findBestMandate(String agentId, String userEmail,
                                                 String requiredScope, BigDecimal amount,
                                                 String categorySlug, String eventSlug) {
        return findBestMandate(agentId, userEmail, requiredScope, amount, categorySlug, eventSlug, null);
    }

    @Transactional(readOnly = true)
    public Optional<MandateDto> findBestMandate(String agentId, String userEmail,
                                                 String requiredScope, BigDecimal amount,
                                                 String categorySlug, String eventSlug,
                                                 String sectionName) {
        List<Mandate> mandates = mandateRepository.findByAgentIdAndUserEmailAndStatus(
                agentId, userEmail, STATUS_ACTIVE);

        Instant now = Instant.now();
        return mandates.stream()
                .filter(m -> m.getExpiresAt() == null || m.getExpiresAt().isAfter(now))
                .filter(m -> validateMandateConstraints(m, requiredScope, amount, categorySlug, eventSlug,
                        sectionName))
                .min(Comparator.comparing(this::mandateSpecificity).reversed()
                        .thenComparing(Mandate::getCreatedAt))
                .map(this::toDto);
    }

    @Transactional
    public void recordSpend(String mandateId, BigDecimal amount) {
        Mandate mandate = mandateRepository.findByMandateIdForUpdate(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(MANDATE_RESOURCE, MANDATE_ID_FIELD, mandateId));
        BigDecimal newTotal = mandate.getTotalSpent().add(amount);
        mandate.setTotalSpent(newTotal);
        mandateRepository.save(mandate);
        log.info("Recorded spend of {} on mandate {}, new total: {}", amount, mandateId, newTotal);
    }

    @Transactional
    public void reverseSpend(String mandateId, BigDecimal amount) {
        Mandate mandate = mandateRepository.findByMandateIdForUpdate(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(MANDATE_RESOURCE, MANDATE_ID_FIELD, mandateId));
        BigDecimal newTotal = mandate.getTotalSpent().subtract(amount);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }
        mandate.setTotalSpent(newTotal);
        mandateRepository.save(mandate);
        log.info("Reversed spend of {} on mandate {}, new total: {}", amount, mandateId, newTotal);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("java:S6809") // Self-invocation is intentional — simple delegation to the full overload
    public boolean validateAction(String agentId, String userEmail, String requiredScope,
                                  BigDecimal amount, String categorySlug, String eventSlug) {
        return validateAction(agentId, userEmail, requiredScope, amount, categorySlug, eventSlug, null);
    }

    @Transactional(readOnly = true)
    public boolean validateAction(String agentId, String userEmail, String requiredScope,
                                  BigDecimal amount, String categorySlug,
                                  String eventSlug, String mandateId) {
        return validateAction(agentId, userEmail, requiredScope, amount, categorySlug, eventSlug, mandateId, null);
    }

    @Transactional(readOnly = true)
    public boolean validateAction(String agentId, String userEmail, String requiredScope,
                                  BigDecimal amount, String categorySlug,
                                  String eventSlug, String mandateId, String sectionName) {
        Mandate mandate = getActiveMandate(agentId, userEmail, mandateId);

        if (mandate == null) {
            log.warn("No active mandate for agent '{}' and user '{}' matching mandate '{}'",
                    agentId, userEmail, mandateId);
            return false;
        }

        return validateMandateConstraints(mandate, requiredScope, amount, categorySlug, eventSlug, sectionName);
    }

    @Transactional(readOnly = true)
    public boolean approvalRequired(String agentId, String userEmail, String mandateId) {
        Mandate mandate = getActiveMandate(agentId, userEmail, mandateId);
        return mandate != null && APPROVAL_MODE_APPROVAL_REQUIRED.equals(mandate.getApprovalMode());
    }

    private boolean validateMandateConstraints(Mandate mandate, String requiredScope,
                                                BigDecimal amount, String categorySlug,
                                                String eventSlug, String sectionName) {
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
                && !mandate.getAllowedCategories().isBlank()
                && !parseCommaSeparated(mandate.getAllowedCategories()).contains(categorySlug.toLowerCase())) {
            log.warn("Mandate {} does not allow category '{}'",
                    mandate.getMandateId(), categorySlug);
            return false;
        }

        if (eventSlug != null && mandate.getAllowedEvents() != null
                && !mandate.getAllowedEvents().isBlank()
                && !parseCommaSeparated(mandate.getAllowedEvents()).contains(eventSlug.toLowerCase())) {
            log.warn("Mandate {} does not allow event '{}'",
                    mandate.getMandateId(), eventSlug);
            return false;
        }

        if (mandate.getAllowedSections() != null && !mandate.getAllowedSections().isBlank()) {
            if (sectionName == null || sectionName.isBlank()) {
                log.warn("Mandate {} requires section context for allowedSections '{}'",
                        mandate.getMandateId(), mandate.getAllowedSections());
                return false;
            }
            if (!parseCommaSeparated(mandate.getAllowedSections()).contains(sectionName.toLowerCase())) {
                log.warn("Mandate {} does not allow section '{}'",
                        mandate.getMandateId(), sectionName);
                return false;
            }
        }

        return true;
    }

    private int mandateSpecificity(Mandate mandate) {
        int score = 0;
        if (mandate.getAllowedEvents() != null && !mandate.getAllowedEvents().isBlank()) {
            score += 4;
        }
        if (mandate.getAllowedCategories() != null && !mandate.getAllowedCategories().isBlank()) {
            score += 2;
        }
        if (mandate.getAllowedSections() != null && !mandate.getAllowedSections().isBlank()) {
            score += 2;
        }
        if (mandate.getMaxSpendPerTransaction() != null) {
            score += 1;
        }
        return score;
    }

    private boolean scopeCovers(String grantedScope, String requiredScope) {
        if ("PURCHASE".equals(grantedScope)) {
            return true;
        }
        return grantedScope.equals(requiredScope);
    }

    /**
     * Normalizes a comma-separated category list (trim, lowercase, drop blanks)
     * and validates each slug against the categories table. Agents frequently
     * pass genres like "jazz" here; rejecting unknown slugs at creation time
     * prevents mandates that can never authorize anything.
     */
    private String normalizeAndValidateCategories(String allowedCategories) {
        if (allowedCategories == null || allowedCategories.isBlank()) {
            return null;
        }
        List<String> requested = Arrays.stream(allowedCategories.split(","))
                .map(String::strip)
                .map(String::toLowerCase)
                .filter(slug -> !slug.isEmpty())
                .distinct()
                .toList();
        if (requested.isEmpty()) {
            return null;
        }
        Set<String> validSlugs = categoryRepository.findAll().stream()
                .map(Category::getSlug)
                .collect(Collectors.toSet());
        List<String> unknown = requested.stream()
                .filter(slug -> !validSlugs.contains(slug))
                .toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown category slug(s): " + unknown
                    + ". Valid category slugs: " + validSlugs.stream().sorted()
                            .collect(Collectors.joining(", "))
                    + ". Categories are event types, not genres.");
        }
        return String.join(",", requested);
    }

    private Set<String> parseCommaSeparated(String value) {
        return Arrays.stream(value.split(","))
                .map(String::strip)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private String normalizeApprovalMode(String approvalMode) {
        if (approvalMode == null || approvalMode.isBlank()) {
            return APPROVAL_MODE_AUTO_PURCHASE;
        }
        String normalized = approvalMode.strip().toUpperCase();
        if (!APPROVAL_MODE_AUTO_PURCHASE.equals(normalized)
                && !APPROVAL_MODE_APPROVAL_REQUIRED.equals(normalized)) {
            throw new IllegalArgumentException(
                    "approvalMode must be AUTO_PURCHASE or APPROVAL_REQUIRED");
        }
        return normalized;
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
                mandate.getAllowedSections(),
                mandate.getApprovalMode(),
                mandate.getStatus(),
                mandate.getExpiresAt(),
                mandate.getCreatedAt()
        );
    }
}
