package com.mockhub.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QrCodeService")
class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
    }

    @Test
    @DisplayName("generateQrCode returns bytes with PNG magic header")
    void generateQrCode_givenContent_returnsPngBytes() {
        byte[] result = qrCodeService.generateQrCode("test-content", 200);

        assertThat(result[0]).isEqualTo((byte) 0x89);
        assertThat(result[1]).isEqualTo((byte) 0x50);
        assertThat(result[2]).isEqualTo((byte) 0x4E);
        assertThat(result[3]).isEqualTo((byte) 0x47);
    }

    @Test
    @DisplayName("generateQrCode returns non-empty byte array")
    void generateQrCode_givenContent_returnsNonEmptyBytes() {
        byte[] result = qrCodeService.generateQrCode("test-content", 200);

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("generateQrCode produces different output for different content")
    void generateQrCode_givenDifferentContent_returnsDifferentBytes() {
        byte[] result1 = qrCodeService.generateQrCode("hello", 200);
        byte[] result2 = qrCodeService.generateQrCode("world", 200);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("generateQrCode handles long JWT-like strings")
    void generateQrCode_givenLongJwtString_succeeds() {
        String longJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                + "a]b".repeat(125);

        byte[] result = qrCodeService.generateQrCode(longJwt, 200);

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("generateQrCode at larger size produces more bytes than smaller size")
    void generateQrCode_givenCustomSize_respectsSize() {
        byte[] small = qrCodeService.generateQrCode("size-test", 100);
        byte[] large = qrCodeService.generateQrCode("size-test", 300);

        assertThat(large.length).isGreaterThan(small.length);
    }
}
