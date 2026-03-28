package com.mockhub.mandate.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MandateService")
class MandateServiceTest {

    @Mock
    private MandateRepository mandateRepository;

    private MandateService mandateService;

    @BeforeEach
    void setUp() {
        mandateService = new MandateService(mandateRepository);
    }

    @Test
    @DisplayName("createMandate creates and returns mandate DTO")
    void createMandate_givenValidRequest_createsAndReturnsDto() {
        CreateMandateRequest request = new CreateMandateRequest(
                "agent-1", "user@example.com", "PURCHASE",
                new BigDecimal("100.00"), new BigDecimal("500.00"),
                "concerts,sports", null, null);

        when(mandateRepository.save(any(Mandate.class))).thenAnswer(invocation -> {
            Mandate mandate = invocation.getArgument(0);
            mandate.setId(1L);
            return mandate;
        });

        MandateDto result = mandateService.createMandate(request);

        assertThat(result.agentId()).isEqualTo("agent-1");
        assertThat(result.userEmail()).isEqualTo("user@example.com");
        assertThat(result.scope()).isEqualTo("PURCHASE");
        assertThat(result.maxSpendPerTransaction()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.maxSpendTotal()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.totalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.remainingBudget()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.mandateId()).isNotNull();
        assertThat(result.allowedCategories()).isEqualTo("concerts,sports");

        ArgumentCaptor<Mandate> captor = ArgumentCaptor.forClass(Mandate.class);
        verify(mandateRepository).save(captor.capture());
        assertThat(captor.getValue().getMandateId()).hasSize(36);
    }

    @Test
    @DisplayName("revokeMandate sets status to REVOKED")
    void revokeMandate_givenExistingMandate_setsStatusRevoked() {
        Mandate mandate = createActiveMandate("mandate-123", "agent-1", "user@example.com");
        when(mandateRepository.findByMandateId("mandate-123")).thenReturn(Optional.of(mandate));
        when(mandateRepository.save(any(Mandate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mandateService.revokeMandate("mandate-123");

        assertThat(mandate.getStatus()).isEqualTo("REVOKED");
        assertThat(mandate.getRevokedAt()).isNotNull();
        verify(mandateRepository).save(mandate);
    }

    @Test
    @DisplayName("revokeMandate throws when mandate not found")
    void revokeMandate_givenNonexistentMandate_throwsResourceNotFound() {
        when(mandateRepository.findByMandateId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mandateService.revokeMandate("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listMandates returns active mandates for user")
    void listMandates_givenUserWithMandates_returnsActiveMandates() {
        Mandate mandate1 = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate1.setId(1L);
        Mandate mandate2 = createActiveMandate("m2", "agent-2", "user@example.com");
        mandate2.setId(2L);
        when(mandateRepository.findByUserEmailAndStatus("user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate1, mandate2));

        List<MandateDto> results = mandateService.listMandates("user@example.com");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).mandateId()).isEqualTo("m1");
        assertThat(results.get(1).mandateId()).isEqualTo("m2");
    }

    @Test
    @DisplayName("getActiveMandate returns mandate when active and not expired")
    void getActiveMandate_givenActiveNonExpired_returnsMandate() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        Mandate result = mandateService.getActiveMandate("agent-1", "user@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getMandateId()).isEqualTo("m1");
    }

    @Test
    @DisplayName("getActiveMandate returns null when expired")
    void getActiveMandate_givenExpiredMandate_returnsNull() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        Mandate result = mandateService.getActiveMandate("agent-1", "user@example.com");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getActiveMandate returns null when no mandates exist")
    void getActiveMandate_givenNoMandates_returnsNull() {
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of());

        Mandate result = mandateService.getActiveMandate("agent-1", "user@example.com");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("validateAction returns true for valid BROWSE mandate")
    void validateAction_givenBrowseMandateAndBrowseScope_returnsTrue() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("BROWSE");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "BROWSE", null, null, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateAction returns false when BROWSE scope tries PURCHASE")
    void validateAction_givenBrowseMandateAndPurchaseScope_returnsFalse() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("BROWSE");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE", null, null, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateAction returns true when PURCHASE scope covers BROWSE")
    void validateAction_givenPurchaseMandateAndBrowseScope_returnsTrue() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "BROWSE", null, null, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateAction returns false when amount exceeds per-transaction limit")
    void validateAction_givenAmountExceedsPerTransactionLimit_returnsFalse() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        mandate.setMaxSpendPerTransaction(new BigDecimal("100.00"));
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE",
                new BigDecimal("150.00"), null, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateAction returns false when projected total exceeds total limit")
    void validateAction_givenProjectedTotalExceedsTotalLimit_returnsFalse() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        mandate.setMaxSpendTotal(new BigDecimal("500.00"));
        mandate.setTotalSpent(new BigDecimal("450.00"));
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE",
                new BigDecimal("75.00"), null, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateAction returns false when category not allowed")
    void validateAction_givenCategoryNotAllowed_returnsFalse() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        mandate.setAllowedCategories("concerts,theater");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE",
                null, "sports", null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateAction returns true when category is allowed")
    void validateAction_givenCategoryAllowed_returnsTrue() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        mandate.setAllowedCategories("concerts,theater");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE",
                null, "concerts", null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateAction returns false when event not allowed")
    void validateAction_givenEventNotAllowed_returnsFalse() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        mandate.setAllowedEvents("taylor-swift,beyonce");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE",
                null, null, "coldplay");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateAction treats empty allowedCategories as unrestricted")
    void validateAction_givenEmptyAllowedCategories_treatsAsUnrestricted() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        mandate.setAllowedCategories("");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE",
                null, "classical-music", null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateAction treats empty allowedEvents as unrestricted")
    void validateAction_givenEmptyAllowedEvents_treatsAsUnrestricted() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setScope("PURCHASE");
        mandate.setAllowedEvents("");
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "PURCHASE",
                null, null, "yo-yo-ma-bach");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateAction returns false when no active mandate")
    void validateAction_givenNoActiveMandate_returnsFalse() {
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of());

        boolean result = mandateService.validateAction(
                "agent-1", "user@example.com", "BROWSE", null, null, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("recordSpend increments total spent")
    void recordSpend_givenExistingMandate_incrementsTotalSpent() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setTotalSpent(new BigDecimal("100.00"));
        when(mandateRepository.findByMandateIdForUpdate("m1")).thenReturn(Optional.of(mandate));
        when(mandateRepository.save(any(Mandate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mandateService.recordSpend("m1", new BigDecimal("50.00"));

        assertThat(mandate.getTotalSpent()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(mandateRepository).save(mandate);
    }

    @Test
    @DisplayName("recordSpend throws when mandate not found")
    void recordSpend_givenNonexistentMandate_throwsResourceNotFound() {
        when(mandateRepository.findByMandateIdForUpdate("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mandateService.recordSpend("nonexistent", BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("reverseSpend decrements total spent")
    void reverseSpend_givenExistingMandate_decrementsTotalSpent() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setTotalSpent(new BigDecimal("150.00"));
        when(mandateRepository.findByMandateIdForUpdate("m1")).thenReturn(Optional.of(mandate));
        when(mandateRepository.save(any(Mandate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mandateService.reverseSpend("m1", new BigDecimal("50.00"));

        assertThat(mandate.getTotalSpent()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(mandateRepository).save(mandate);
    }

    @Test
    @DisplayName("reverseSpend floors at zero when amount exceeds total spent")
    void reverseSpend_givenAmountExceedingTotalSpent_floorsAtZero() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setTotalSpent(new BigDecimal("30.00"));
        when(mandateRepository.findByMandateIdForUpdate("m1")).thenReturn(Optional.of(mandate));
        when(mandateRepository.save(any(Mandate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mandateService.reverseSpend("m1", new BigDecimal("50.00"));

        assertThat(mandate.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(mandateRepository).save(mandate);
    }

    @Test
    @DisplayName("reverseSpend throws when mandate not found")
    void reverseSpend_givenNonexistentMandate_throwsResourceNotFoundException() {
        when(mandateRepository.findByMandateIdForUpdate("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mandateService.reverseSpend("nonexistent", BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- findBestMandate ---

    @Test
    @DisplayName("findBestMandate returns matching mandate when constraints pass")
    void findBestMandate_givenMatchingMandate_returnsMandate() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setId(1L);
        mandate.setScope("PURCHASE");
        mandate.setMaxSpendPerTransaction(new BigDecimal("200.00"));
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        Optional<MandateDto> result = mandateService.findBestMandate(
                "agent-1", "user@example.com", "PURCHASE",
                new BigDecimal("100.00"), null, null);

        assertThat(result).isPresent();
        assertThat(result.get().mandateId()).isEqualTo("m1");
        assertThat(result.get().scope()).isEqualTo("PURCHASE");
    }

    @Test
    @DisplayName("findBestMandate returns empty when no mandates exist")
    void findBestMandate_givenNoMandates_returnsEmpty() {
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of());

        Optional<MandateDto> result = mandateService.findBestMandate(
                "agent-1", "user@example.com", "PURCHASE",
                new BigDecimal("50.00"), null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findBestMandate filters out expired mandates")
    void findBestMandate_givenExpiredMandate_returnsEmpty() {
        Mandate mandate = createActiveMandate("m1", "agent-1", "user@example.com");
        mandate.setId(1L);
        mandate.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(mandate));

        Optional<MandateDto> result = mandateService.findBestMandate(
                "agent-1", "user@example.com", "PURCHASE",
                null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findBestMandate - given multiple matching mandates - returns most specific")
    void findBestMandate_givenMultipleMatchingMandates_returnsMostSpecific() {
        Mandate broadMandate = createActiveMandate("m-broad", "agent-1", "user@example.com");
        broadMandate.setScope("PURCHASE");
        broadMandate.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));

        Mandate specificMandate = createActiveMandate("m-specific", "agent-1", "user@example.com");
        specificMandate.setScope("PURCHASE");
        specificMandate.setAllowedEvents("rock-festival");
        specificMandate.setAllowedCategories("concerts");
        specificMandate.setMaxSpendPerTransaction(new BigDecimal("200.00"));
        specificMandate.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(mandateRepository.findByAgentIdAndUserEmailAndStatus("agent-1", "user@example.com", "ACTIVE"))
                .thenReturn(List.of(broadMandate, specificMandate));

        Optional<MandateDto> result = mandateService.findBestMandate(
                "agent-1", "user@example.com", "PURCHASE",
                new BigDecimal("100.00"), "concerts", "rock-festival");

        assertThat(result).isPresent();
        assertThat(result.get().mandateId()).isEqualTo("m-specific");
    }

    private Mandate createActiveMandate(String mandateId, String agentId, String userEmail) {
        Mandate mandate = new Mandate();
        mandate.setMandateId(mandateId);
        mandate.setAgentId(agentId);
        mandate.setUserEmail(userEmail);
        mandate.setScope("PURCHASE");
        mandate.setStatus("ACTIVE");
        mandate.setTotalSpent(BigDecimal.ZERO);
        return mandate;
    }
}
