package com.mockhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import com.mockhub.auth.dto.AuthResponse;

/**
 * Base class for integration tests using a shared Testcontainers PostgreSQL instance.
 * The container is started once and reused across all test classes to avoid port
 * conflicts with the cached Spring context.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration," +
                        "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration," +
                        "org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration," +
                        "org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration," +
                        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration"
        }
)
@ActiveProfiles({"test", "mock-payment", "mock-sms"})
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:17")
                .withDatabaseName("mockhub")
                .withUsername("mockhub")
                .withPassword("mockhub");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

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
