package com.mockhub.ticket.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketSigningService")
class TicketSigningServiceTest {

    private static final String TEST_SECRET =
            "dGVzdC10aWNrZXQtc2lnbmluZy1zZWNyZXQtZm9yLXVuaXQtdGVzdHMtbXVzdC1iZS1sb25n";

    private TicketSigningService service;

    @BeforeEach
    void setUp() {
        service = new TicketSigningService(TEST_SECRET);
    }

    @Test
    @DisplayName("generateToken returns non-empty string for valid ticket data")
    void generateToken_givenValidTicketData_returnsNonEmptyString() {
        String token = service.generateToken("ORD-001", 42L, "taylor-swift-eras",
                "Floor A", "R1", "S5");

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("generateToken produces token with expected claims on round-trip")
    void generateToken_givenTicketData_tokenContainsExpectedClaims() {
        String token = service.generateToken("ORD-002", 99L, "coldplay-world-tour",
                "Balcony", "B3", "12");

        Claims claims = service.verifyToken(token);

        assertThat(claims.getSubject()).isEqualTo("ORD-002");
        assertThat(claims.get("tic", Long.class)).isEqualTo(99L);
        assertThat(claims.get("evt", String.class)).isEqualTo("coldplay-world-tour");
        assertThat(claims.get("sec", String.class)).isEqualTo("Balcony");
        assertThat(claims.get("row", String.class)).isEqualTo("B3");
        assertThat(claims.get("seat", String.class)).isEqualTo("12");
    }

    @Test
    @DisplayName("verifyToken returns correct claims for a valid token")
    void verifyToken_givenValidToken_returnsClaims() {
        String token = service.generateToken("ORD-100", 7L, "adele-live",
                "VIP", "A1", "1");

        Claims claims = service.verifyToken(token);

        assertThat(claims.getSubject()).isEqualTo("ORD-100");
        assertThat(claims.get("tic", Long.class)).isEqualTo(7L);
        assertThat(claims.get("evt", String.class)).isEqualTo("adele-live");
        assertThat(claims.get("sec", String.class)).isEqualTo("VIP");
        assertThat(claims.get("row", String.class)).isEqualTo("A1");
        assertThat(claims.get("seat", String.class)).isEqualTo("1");
        assertThat(claims.getIssuedAt()).isNotNull();
    }

    @Test
    @DisplayName("verifyToken throws JwtException for tampered token")
    void verifyToken_givenTamperedToken_throwsJwtException() {
        String token = service.generateToken("ORD-003", 10L, "beyonce-tour",
                "Section 1", "C2", "8");

        // Tamper with the token by altering a character in the payload (middle segment)
        char[] chars = token.toCharArray();
        int midpoint = token.indexOf('.') + 5;
        chars[midpoint] = chars[midpoint] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);

        assertThatThrownBy(() -> service.verifyToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("verifyToken throws JwtException when verified with wrong secret")
    void verifyToken_givenWrongSecret_throwsJwtException() {
        String differentSecret =
                "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXdyb25nLXNlY3JldC1jYXNl";
        TicketSigningService otherService = new TicketSigningService(differentSecret);

        String token = service.generateToken("ORD-004", 20L, "drake-concert",
                "Upper Deck", "D1", "3");

        assertThatThrownBy(() -> otherService.verifyToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("generateToken omits row and seat claims when null (GA tickets)")
    void generateToken_givenNullRowAndSeat_omitsRowAndSeatClaims() {
        String token = service.generateToken("ORD-005", 50L, "festival-ga",
                "General Admission", null, null);

        Claims claims = service.verifyToken(token);

        assertThat(claims.getSubject()).isEqualTo("ORD-005");
        assertThat(claims.get("tic", Long.class)).isEqualTo(50L);
        assertThat(claims.get("evt", String.class)).isEqualTo("festival-ga");
        assertThat(claims.get("sec", String.class)).isEqualTo("General Admission");
        assertThat(claims.get("row")).isNull();
        assertThat(claims.get("seat")).isNull();
    }

    @Test
    @DisplayName("generateToken includes row and seat claims when provided")
    void generateToken_givenAllFields_includesRowAndSeatClaims() {
        String token = service.generateToken("ORD-006", 77L, "rolling-stones",
                "Orchestra", "F5", "22");

        Claims claims = service.verifyToken(token);

        assertThat(claims.get("row", String.class)).isEqualTo("F5");
        assertThat(claims.get("seat", String.class)).isEqualTo("22");
    }
}
