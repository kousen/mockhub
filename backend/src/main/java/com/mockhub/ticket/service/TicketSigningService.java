package com.mockhub.ticket.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TicketSigningService {

    private final SecretKey secretKey;

    public TicketSigningService(@Value("${mockhub.ticket.signing-secret}") String signingSecret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(signingSecret));
    }

    public String generateToken(String orderNumber, Long ticketId, String eventSlug,
                                String sectionName, String rowLabel, String seatNumber) {
        JwtBuilder builder = Jwts.builder()
                .subject(orderNumber)
                .claim("tic", ticketId)
                .claim("evt", eventSlug)
                .claim("sec", sectionName);

        if (rowLabel != null) {
            builder.claim("row", rowLabel);
        }
        if (seatNumber != null) {
            builder.claim("seat", seatNumber);
        }

        return builder
                .issuedAt(new Date())
                .signWith(secretKey)
                .compact();
    }

    public Claims verifyToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
