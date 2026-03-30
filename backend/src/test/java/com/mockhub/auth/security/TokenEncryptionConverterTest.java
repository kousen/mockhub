package com.mockhub.auth.security;

import java.util.Base64;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenEncryptionConverterTest {

    private TokenEncryptionConverter converter;

    @BeforeEach
    void setUp() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        String base64Key = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
        converter = new TokenEncryptionConverter(base64Key);
    }

    @Test
    @DisplayName("encrypt and decrypt - round trip - returns original plaintext")
    void encryptDecrypt_roundTrip_returnsOriginalPlaintext() {
        String token = "BQDj7s0xKz9Pq4mN-spotify-access-token-value";

        String encrypted = converter.convertToDatabaseColumn(token);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertEquals(token, decrypted);
    }

    @Test
    @DisplayName("encrypt - null input - returns null")
    void encrypt_nullInput_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    @DisplayName("decrypt - null input - returns null")
    void decrypt_nullInput_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    @DisplayName("encrypt - same plaintext twice - produces different ciphertext (random IV)")
    void encrypt_samePlaintextTwice_producesDifferentCiphertext() {
        String token = "same-token-value";

        String encrypted1 = converter.convertToDatabaseColumn(token);
        String encrypted2 = converter.convertToDatabaseColumn(token);

        assertNotEquals(encrypted1, encrypted2, "Random IV should produce unique ciphertext");
    }

    @Test
    @DisplayName("decrypt - tampered ciphertext - throws exception")
    void decrypt_tamperedCiphertext_throwsException() {
        String token = "valid-token";
        String encrypted = converter.convertToDatabaseColumn(token);

        // Tamper with the ciphertext
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        decoded[decoded.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(decoded);

        assertThrows(IllegalStateException.class,
                () -> converter.convertToEntityAttribute(tampered));
    }

    @Test
    @DisplayName("encrypt - no encryption key configured - throws exception")
    void encrypt_noKeyConfigured_throwsException() {
        TokenEncryptionConverter noKeyConverter = new TokenEncryptionConverter("");

        assertThrows(IllegalStateException.class,
                () -> noKeyConverter.convertToDatabaseColumn("some-token"));
    }

    @Test
    @DisplayName("decrypt - no encryption key configured - throws exception")
    void decrypt_noKeyConfigured_throwsException() {
        TokenEncryptionConverter noKeyConverter = new TokenEncryptionConverter("");

        assertThrows(IllegalStateException.class,
                () -> noKeyConverter.convertToEntityAttribute("some-encrypted-value"));
    }

    @Test
    @DisplayName("encrypt and decrypt - empty string - round trips correctly")
    void encryptDecrypt_emptyString_roundTripsCorrectly() {
        String encrypted = converter.convertToDatabaseColumn("");
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertEquals("", decrypted);
    }

    @Test
    @DisplayName("encrypt and decrypt - long token - round trips correctly")
    void encryptDecrypt_longToken_roundTripsCorrectly() {
        String longToken = "a".repeat(2000);

        String encrypted = converter.convertToDatabaseColumn(longToken);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertEquals(longToken, decrypted);
    }
}
