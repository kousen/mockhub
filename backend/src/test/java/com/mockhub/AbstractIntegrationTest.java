package com.mockhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.mockhub.auth.dto.AuthResponse;

/**
 * Base class for integration tests using Testcontainers PostgreSQL.
 * The test profile (application-test.yml) configures the Testcontainers JDBC driver,
 * so no explicit @Container or @DynamicPropertySource is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Registers a new user and returns the auth response containing a JWT token.
     */
    protected AuthResponse registerUser(String email, String password,
                                         String firstName, String lastName) {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s",
                    "firstName": "%s",
                    "lastName": "%s"
                }
                """, email, password, firstName, lastName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(body, headers),
                AuthResponse.class);

        return response.getBody();
    }

    /**
     * Logs in an existing user and returns the auth response containing a JWT token.
     */
    protected AuthResponse loginUser(String email, String password) {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(body, headers),
                AuthResponse.class);

        return response.getBody();
    }

    /**
     * Creates HTTP headers with a Bearer token for authenticated requests.
     */
    protected HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
