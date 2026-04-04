package com.mockhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import com.mockhub.auth.security.CookieOAuth2AuthorizationRequestRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.OAuth2AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Optional<OAuth2AuthenticationSuccessHandler> oauth2SuccessHandler;
    private final Optional<CookieOAuth2AuthorizationRequestRepository> oauth2AuthorizationRequestRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          Optional<OAuth2AuthenticationSuccessHandler> oauth2SuccessHandler,
                          Optional<CookieOAuth2AuthorizationRequestRepository> oauth2AuthorizationRequestRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2AuthorizationRequestRepository = oauth2AuthorizationRequestRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints (login, register, refresh, OAuth2 — but NOT /me)
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/oauth2/exchange").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .requestMatchers("/oauth2/authorization/**").permitAll()
                        // Public catalog endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/venues/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags").permitAll()
                        .requestMatchers("/api/v1/search/**").permitAll()
                        // Public Spotify metadata
                        .requestMatchers(HttpMethod.GET, "/api/v1/spotify/**").permitAll()
                        // Public AI endpoints
                        .requestMatchers("/api/v1/chat").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recommendations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/images/**").permitAll()
                        // Public ticket endpoints (verification + public ticket view)
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/*/*/qr").permitAll()
                        // API documentation and agent discovery
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/llms.txt").permitAll()
                        // MCP server endpoints (API key or OAuth2 filter handles auth)
                        .requestMatchers("/mcp/**").permitAll()
                        // ACP endpoints (API key filter handles auth)
                        .requestMatchers("/acp/**").permitAll()
                        // OAuth2 authorization server endpoints (handled by separate SecurityFilterChain)
                        .requestMatchers("/oauth2/**", "/.well-known/**").permitAll()
                        // Static frontend resources and SPA routes (React served from classpath:/static/)
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/favicon.svg").permitAll()
                        .requestMatchers("/login", "/register", "/events/**", "/sell", "/my/**",
                                "/cart", "/checkout", "/orders/**", "/favorites", "/admin/**",
                                "/verify", "/tickets/**", "/auth/callback").permitAll()
                        // Actuator health and error page
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        if (oauth2SuccessHandler.isPresent() && oauth2AuthorizationRequestRepository.isPresent()) {
            http.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(auth -> auth
                            .authorizationRequestRepository(oauth2AuthorizationRequestRepository.get()))
                    .successHandler(oauth2SuccessHandler.get()));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
