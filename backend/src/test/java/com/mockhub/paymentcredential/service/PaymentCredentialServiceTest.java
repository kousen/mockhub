package com.mockhub.paymentcredential.service;

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

import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.paymentcredential.dto.CreatePaymentCredentialRequest;
import com.mockhub.paymentcredential.dto.PaymentCredentialDto;
import com.mockhub.paymentcredential.entity.PaymentCredential;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;
import com.mockhub.paymentcredential.entity.PaymentCredentialUsage;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCredentialService")
class PaymentCredentialServiceTest {

    @Mock
    private PaymentCredentialRepository paymentCredentialRepository;

    private PaymentCredentialService paymentCredentialService;

    @BeforeEach
    void setUp() {
        paymentCredentialService = new PaymentCredentialService(paymentCredentialRepository);
    }

    @Test
    @DisplayName("issueCredential creates active one-time mock credential with defaults")
    void issueCredential_givenMinimalRequest_createsActiveOneTimeMockCredential() {
        CreatePaymentCredentialRequest request = new CreatePaymentCredentialRequest(
                " buyer@example.com ", " agent-1 ", new BigDecimal("75"), null, null, null, null);
        when(paymentCredentialRepository.save(any(PaymentCredential.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentCredentialDto result = paymentCredentialService.issueCredential(request);

        assertThat(result.credentialId()).hasSize(36);
        assertThat(result.userEmail()).isEqualTo("buyer@example.com");
        assertThat(result.agentId()).isEqualTo("agent-1");
        assertThat(result.allowedMerchant()).isEqualTo(PaymentCredentialService.MOCKHUB_MERCHANT);
        assertThat(result.maxAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.usage()).isEqualTo("ONE_TIME");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.backingPaymentMethod()).isEqualTo("mock");

        ArgumentCaptor<PaymentCredential> captor = ArgumentCaptor.forClass(PaymentCredential.class);
        verify(paymentCredentialRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentCredentialStatus.ACTIVE);
    }

    @Test
    @DisplayName("listCredentials returns user credentials newest first from repository order")
    void listCredentials_givenUserEmail_returnsDtos() {
        PaymentCredential first = activeCredential("cred-1");
        PaymentCredential second = activeCredential("cred-2");
        when(paymentCredentialRepository.findByUserEmailOrderByCreatedAtDesc("buyer@example.com"))
                .thenReturn(List.of(first, second));

        List<PaymentCredentialDto> results = paymentCredentialService.listCredentials("buyer@example.com");

        assertThat(results).extracting(PaymentCredentialDto::credentialId)
                .containsExactly("cred-1", "cred-2");
    }

    @Test
    @DisplayName("authorizeForPayment accepts active matching credential")
    void authorizeForPayment_givenActiveMatchingCredential_returnsDto() {
        PaymentCredential credential = activeCredential("cred-123");
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        PaymentCredentialDto result = paymentCredentialService.authorizeForPayment(
                "cred-123", "buyer@example.com", "agent-1",
                new BigDecimal("55.00"), "USD", "mock", "MH-1");

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.credentialId()).isEqualTo("cred-123");
    }

    @Test
    @DisplayName("authorizeForPayment rejects over-limit amount")
    void authorizeForPayment_givenOverLimitAmount_throwsConflictException() {
        PaymentCredential credential = activeCredential("cred-123");
        credential.setMaxAmount(new BigDecimal("50.00"));
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> paymentCredentialService.authorizeForPayment(
                "cred-123", "buyer@example.com", "agent-1",
                new BigDecimal("55.00"), "USD", "mock", "MH-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("exceeds payment credential limit");
    }

    @Test
    @DisplayName("authorizeForPayment rejects revoked credential")
    void authorizeForPayment_givenRevokedCredential_throwsConflictException() {
        PaymentCredential credential = activeCredential("cred-123");
        credential.setStatus(PaymentCredentialStatus.REVOKED);
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> paymentCredentialService.authorizeForPayment(
                "cred-123", "buyer@example.com", "agent-1",
                new BigDecimal("55.00"), "USD", "mock", "MH-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("REVOKED");
    }

    @Test
    @DisplayName("authorizeForPayment expires active credential past expiration")
    void authorizeForPayment_givenExpiredCredential_marksExpiredAndThrowsConflictException() {
        PaymentCredential credential = activeCredential("cred-123");
        credential.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> paymentCredentialService.authorizeForPayment(
                "cred-123", "buyer@example.com", "agent-1",
                new BigDecimal("55.00"), "USD", "mock", "MH-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("EXPIRED");
        assertThat(credential.getStatus()).isEqualTo(PaymentCredentialStatus.EXPIRED);
    }

    @Test
    @DisplayName("authorizeForPayment rejects wrong agent")
    void authorizeForPayment_givenWrongAgent_throwsConflictException() {
        PaymentCredential credential = activeCredential("cred-123");
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> paymentCredentialService.authorizeForPayment(
                "cred-123", "buyer@example.com", "other-agent",
                new BigDecimal("55.00"), "USD", "mock", "MH-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("does not authorize agent");
    }

    @Test
    @DisplayName("authorizeForPayment rejects consumed credential for same order when user does not match")
    void authorizeForPayment_givenConsumedCredentialForSameOrderAndWrongUser_throwsConflictException() {
        PaymentCredential credential = activeCredential("cred-123");
        credential.setStatus(PaymentCredentialStatus.CONSUMED);
        credential.setConsumedByOrderNumber("MH-1");
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> paymentCredentialService.authorizeForPayment(
                "cred-123", "other@example.com", "agent-1",
                new BigDecimal("55.00"), "USD", "mock", "MH-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("does not belong to user");
    }

    @Test
    @DisplayName("consumeForPayment consumes one-time credential exactly once")
    void consumeForPayment_givenOneTimeCredential_consumesExactlyOnce() {
        PaymentCredential credential = activeCredential("cred-123");
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        PaymentCredentialDto first = paymentCredentialService.consumeForPayment("cred-123", "MH-1");
        PaymentCredentialDto second = paymentCredentialService.consumeForPayment("cred-123", "MH-1");

        assertThat(first.status()).isEqualTo("CONSUMED");
        assertThat(second.status()).isEqualTo("CONSUMED");
        assertThat(credential.getConsumedByOrderNumber()).isEqualTo("MH-1");
        assertThat(credential.getConsumedAt()).isNotNull();
    }

    @Test
    @DisplayName("consumeForPayment leaves reusable credential active")
    void consumeForPayment_givenReusableCredential_keepsActiveAndTracksUse() {
        PaymentCredential credential = activeCredential("cred-123");
        credential.setUsage(PaymentCredentialUsage.REUSABLE);
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        PaymentCredentialDto result = paymentCredentialService.consumeForPayment("cred-123", "MH-1");

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(credential.getConsumedByOrderNumber()).isNull();
        assertThat(credential.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("consumeForPayment rejects reusable credential that is no longer active")
    void consumeForPayment_givenReusableRevokedCredential_throwsConflictException() {
        PaymentCredential credential = activeCredential("cred-123");
        credential.setUsage(PaymentCredentialUsage.REUSABLE);
        credential.setStatus(PaymentCredentialStatus.REVOKED);
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> paymentCredentialService.consumeForPayment("cred-123", "MH-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("REVOKED");
    }

    @Test
    @DisplayName("revokeCredential revokes active user-owned credential")
    void revokeCredential_givenActiveCredential_setsRevokedStatus() {
        PaymentCredential credential = activeCredential("cred-123");
        when(paymentCredentialRepository.findByCredentialIdForUpdate("cred-123"))
                .thenReturn(Optional.of(credential));

        PaymentCredentialDto result = paymentCredentialService.revokeCredential("cred-123", "buyer@example.com");

        assertThat(result.status()).isEqualTo("REVOKED");
        assertThat(credential.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("authorizeForPayment throws ResourceNotFoundException for unknown credential")
    void authorizeForPayment_givenUnknownCredential_throwsResourceNotFoundException() {
        when(paymentCredentialRepository.findByCredentialIdForUpdate("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCredentialService.authorizeForPayment(
                "missing", "buyer@example.com", "agent-1",
                new BigDecimal("55.00"), "USD", "mock", "MH-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private PaymentCredential activeCredential(String credentialId) {
        PaymentCredential credential = new PaymentCredential();
        credential.setId(1L);
        credential.setCredentialId(credentialId);
        credential.setUserEmail("buyer@example.com");
        credential.setAgentId("agent-1");
        credential.setAllowedMerchant(PaymentCredentialService.MOCKHUB_MERCHANT);
        credential.setMaxAmount(new BigDecimal("100.00"));
        credential.setCurrency("USD");
        credential.setUsage(PaymentCredentialUsage.ONE_TIME);
        credential.setStatus(PaymentCredentialStatus.ACTIVE);
        credential.setBackingPaymentMethod("mock");
        return credential;
    }
}
