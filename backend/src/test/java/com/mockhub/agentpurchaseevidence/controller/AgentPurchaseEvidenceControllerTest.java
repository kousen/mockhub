package com.mockhub.agentpurchaseevidence.controller;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto;
import com.mockhub.agentpurchaseevidence.service.AgentPurchaseEvidenceService;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.security.SecurityUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPurchaseEvidenceControllerTest {

    private static final String ORDER_NUMBER = "MH-20260522-0001";

    @Mock
    private AgentPurchaseEvidenceService evidenceService;

    private AgentPurchaseEvidenceController controller;

    @BeforeEach
    void setUp() {
        controller = new AgentPurchaseEvidenceController(evidenceService);
    }

    @Test
    @DisplayName("getEvidence - owner principal - delegates with admin false")
    void getEvidence_givenOwnerPrincipal_delegatesWithAdminFalse() {
        SecurityUser securityUser = securityUser("buyer@example.com", Set.of("ROLE_BUYER"));
        AgentPurchaseEvidenceDto dto = evidenceDto("buyer@example.com");
        when(evidenceService.getEvidence(ORDER_NUMBER, "buyer@example.com", false)).thenReturn(dto);

        ResponseEntity<AgentPurchaseEvidenceDto> response = controller.getEvidence(securityUser, ORDER_NUMBER);

        assertThat(response.getBody()).isSameAs(dto);
        verify(evidenceService).getEvidence(ORDER_NUMBER, "buyer@example.com", false);
    }

    @Test
    @DisplayName("getEvidence - admin principal - delegates with admin true")
    void getEvidence_givenAdminPrincipal_delegatesWithAdminTrue() {
        SecurityUser securityUser = securityUser("admin@example.com", Set.of("ROLE_ADMIN"));
        AgentPurchaseEvidenceDto dto = evidenceDto("buyer@example.com");
        when(evidenceService.getEvidence(ORDER_NUMBER, "admin@example.com", true)).thenReturn(dto);

        ResponseEntity<AgentPurchaseEvidenceDto> response = controller.getEvidence(securityUser, ORDER_NUMBER);

        assertThat(response.getBody()).isSameAs(dto);
        verify(evidenceService).getEvidence(ORDER_NUMBER, "admin@example.com", true);
    }

    private SecurityUser securityUser(String email, Set<String> roles) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRoles(roles.stream().map(Role::new).collect(java.util.stream.Collectors.toSet()));
        return new SecurityUser(user);
    }

    private AgentPurchaseEvidenceDto evidenceDto(String userEmail) {
        return new AgentPurchaseEvidenceDto(
                ORDER_NUMBER,
                userEmail,
                "agent-1",
                "CONFIRMED",
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of());
    }
}
