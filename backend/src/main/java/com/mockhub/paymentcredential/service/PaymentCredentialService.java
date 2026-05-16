package com.mockhub.paymentcredential.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.paymentcredential.dto.CreatePaymentCredentialRequest;
import com.mockhub.paymentcredential.dto.PaymentCredentialDto;
import com.mockhub.paymentcredential.entity.PaymentCredential;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;
import com.mockhub.paymentcredential.entity.PaymentCredentialUsage;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;

@Service
public class PaymentCredentialService {

    public static final String MOCKHUB_MERCHANT = "MOCKHUB";

    private static final Logger log = LoggerFactory.getLogger(PaymentCredentialService.class);
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final String CURRENCY_USD = "USD";
    private static final String PAYMENT_METHOD_MOCK = "mock";
    private static final String PAYMENT_CREDENTIAL_RESOURCE = "PaymentCredential";
    private static final String CREDENTIAL_ID_FIELD = "credentialId";

    private final PaymentCredentialRepository paymentCredentialRepository;

    public PaymentCredentialService(PaymentCredentialRepository paymentCredentialRepository) {
        this.paymentCredentialRepository = paymentCredentialRepository;
    }

    @Transactional
    public PaymentCredentialDto issueCredential(CreatePaymentCredentialRequest request) {
        PaymentCredential credential = new PaymentCredential();
        credential.setCredentialId(UUID.randomUUID().toString());
        credential.setUserEmail(normalizeRequired(request.userEmail(), "User email"));
        credential.setAgentId(normalizeRequired(request.agentId(), "Agent ID"));
        credential.setAllowedMerchant(MOCKHUB_MERCHANT);
        credential.setMaxAmount(normalizeAmount(request.maxAmount()));
        credential.setCurrency(normalizeCredentialCurrency(request.currency()));
        credential.setUsage(normalizeUsage(request.usage()));
        credential.setStatus(PaymentCredentialStatus.ACTIVE);
        credential.setBackingPaymentMethod(normalizeBackingPaymentMethod(request.backingPaymentMethod()));
        credential.setExpiresAt(request.expiresAt());

        PaymentCredential saved = paymentCredentialRepository.save(credential);
        log.info("Issued payment credential {} for agent '{}'",
                saved.getCredentialId(), saved.getAgentId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentCredentialDto> listCredentials(String userEmail) {
        return paymentCredentialRepository.findByUserEmailOrderByCreatedAtDesc(normalizeRequired(userEmail, "User email"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PaymentCredentialDto revokeCredential(String credentialId, String userEmail) {
        PaymentCredential credential = getCredentialForUpdate(credentialId);
        String normalizedUserEmail = normalizeRequired(userEmail, "User email");
        if (!credential.getUserEmail().equals(normalizedUserEmail)) {
            throw new ConflictException("Payment credential does not belong to user " + normalizedUserEmail);
        }
        if (credential.getStatus() == PaymentCredentialStatus.CONSUMED) {
            throw new ConflictException("Consumed payment credential cannot be revoked");
        }
        if (credential.getStatus() != PaymentCredentialStatus.REVOKED) {
            credential.setStatus(PaymentCredentialStatus.REVOKED);
            credential.setRevokedAt(Instant.now());
        }
        return toDto(credential);
    }

    @Transactional(noRollbackFor = ConflictException.class)
    public PaymentCredentialDto authorizeForPayment(String credentialId, String userEmail, String agentId,
                                                    BigDecimal amount, String currency, String paymentMethod,
                                                    String orderNumber) {
        PaymentCredential credential = getCredentialForUpdate(credentialId);
        validateActiveCredential(credential, userEmail, agentId, amount, currency, paymentMethod, orderNumber);
        return toDto(credential);
    }

    @Transactional(noRollbackFor = ConflictException.class)
    public PaymentCredentialDto consumeForPayment(String credentialId, String orderNumber) {
        PaymentCredential credential = getCredentialForUpdate(credentialId);
        String normalizedOrderNumber = normalizeRequired(orderNumber, "Order number");

        if (credential.getUsage() == PaymentCredentialUsage.ONE_TIME) {
            if (credential.getStatus() == PaymentCredentialStatus.CONSUMED
                    && normalizedOrderNumber.equals(credential.getConsumedByOrderNumber())) {
                return toDto(credential);
            }
        }

        expireIfNeeded(credential);

        if (credential.getStatus() != PaymentCredentialStatus.ACTIVE) {
            throw new ConflictException("Payment credential " + credential.getCredentialId()
                    + " is " + credential.getStatus());
        }

        if (credential.getUsage() == PaymentCredentialUsage.ONE_TIME) {
            credential.setStatus(PaymentCredentialStatus.CONSUMED);
            credential.setConsumedAt(Instant.now());
            credential.setConsumedByOrderNumber(normalizedOrderNumber);
        }

        credential.setLastUsedAt(Instant.now());
        return toDto(credential);
    }

    private void validateActiveCredential(PaymentCredential credential, String userEmail, String agentId,
                                          BigDecimal amount, String currency, String paymentMethod,
                                          String orderNumber) {
        String normalizedUserEmail = normalizeRequired(userEmail, "User email");
        String normalizedAgentId = normalizeRequired(agentId, "Agent ID");
        BigDecimal normalizedAmount = normalizeAmount(amount);
        String normalizedCurrency = normalizePaymentCurrency(currency);
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizedOrderNumber = normalizeRequired(orderNumber, "Order number");

        expireIfNeeded(credential);

        if (!credential.getUserEmail().equals(normalizedUserEmail)) {
            throw new ConflictException("Payment credential does not belong to user " + normalizedUserEmail);
        }
        if (!credential.getAgentId().equals(normalizedAgentId)) {
            throw new ConflictException("Payment credential does not authorize agent " + normalizedAgentId);
        }
        if (!MOCKHUB_MERCHANT.equals(credential.getAllowedMerchant())) {
            throw new ConflictException("Payment credential is not valid for MockHub");
        }
        if (!credential.getCurrency().equals(normalizedCurrency)) {
            throw new ConflictException("Payment credential currency " + credential.getCurrency()
                    + " does not match " + normalizedCurrency);
        }
        if (!credential.getBackingPaymentMethod().equals(normalizedPaymentMethod)) {
            throw new ConflictException("Payment credential is bound to payment method "
                    + credential.getBackingPaymentMethod());
        }
        if (normalizedAmount.compareTo(credential.getMaxAmount()) > 0) {
            throw new ConflictException("Order total " + normalizedAmount
                    + " exceeds payment credential limit " + credential.getMaxAmount());
        }
        if (credential.getStatus() == PaymentCredentialStatus.CONSUMED
                && normalizedOrderNumber.equals(credential.getConsumedByOrderNumber())) {
            return;
        }
        if (credential.getStatus() != PaymentCredentialStatus.ACTIVE) {
            throw new ConflictException("Payment credential " + credential.getCredentialId()
                    + " is " + credential.getStatus());
        }
    }

    private void expireIfNeeded(PaymentCredential credential) {
        if (credential.getExpiresAt() != null && !credential.getExpiresAt().isAfter(Instant.now())
                && credential.getStatus() == PaymentCredentialStatus.ACTIVE) {
            credential.setStatus(PaymentCredentialStatus.EXPIRED);
        }
    }

    private PaymentCredential getCredentialForUpdate(String credentialId) {
        String normalizedCredentialId = normalizeRequired(credentialId, "Payment credential ID");
        return paymentCredentialRepository.findByCredentialIdForUpdate(normalizedCredentialId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        PAYMENT_CREDENTIAL_RESOURCE, CREDENTIAL_ID_FIELD, normalizedCredentialId));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be at least 0.01");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCredentialCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return CURRENCY_USD;
        }
        String normalized = currency.strip().toUpperCase();
        if (!CURRENCY_USD.equals(normalized)) {
            throw new IllegalArgumentException("Only USD payment credentials are supported");
        }
        return normalized;
    }

    private String normalizePaymentCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return CURRENCY_USD;
        }
        String normalized = currency.strip().toUpperCase();
        if (!CURRENCY_USD.equals(normalized)) {
            throw new ConflictException("Only USD payment credentials are supported");
        }
        return normalized;
    }

    private PaymentCredentialUsage normalizeUsage(String usage) {
        if (usage == null || usage.isBlank()) {
            return PaymentCredentialUsage.ONE_TIME;
        }
        return PaymentCredentialUsage.valueOf(usage.strip().toUpperCase());
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return PAYMENT_METHOD_MOCK;
        }
        return paymentMethod.strip().toLowerCase();
    }

    private String normalizeBackingPaymentMethod(String paymentMethod) {
        String normalized = normalizePaymentMethod(paymentMethod);
        if (!PAYMENT_METHOD_MOCK.equals(normalized)) {
            throw new IllegalArgumentException("Only mock-backed payment credentials are supported");
        }
        return normalized;
    }

    private PaymentCredentialDto toDto(PaymentCredential credential) {
        return new PaymentCredentialDto(
                credential.getId(),
                credential.getCredentialId(),
                credential.getUserEmail(),
                credential.getAgentId(),
                credential.getAllowedMerchant(),
                credential.getMaxAmount(),
                credential.getCurrency(),
                credential.getUsage().name(),
                credential.getStatus().name(),
                credential.getBackingPaymentMethod(),
                credential.getExpiresAt(),
                credential.getRevokedAt(),
                credential.getConsumedAt(),
                credential.getConsumedByOrderNumber(),
                credential.getLastUsedAt(),
                credential.getCreatedAt()
        );
    }
}
