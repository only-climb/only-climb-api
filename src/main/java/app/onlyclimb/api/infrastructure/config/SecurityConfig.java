package app.onlyclimb.api.infrastructure.config;

import app.onlyclimb.api.infrastructure.adapter.in.web.auth.ClerkJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security configuration.
 *
 * <p>The API is stateless and consumes Clerk-issued JWTs as bearer tokens via
 * the OAuth2 Resource Server. The Clerk webhook endpoint is public and
 * authenticates payloads by verifying the Svix signature (see
 * {@code SvixSignatureVerifier}).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(ClerkProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public infra / docs
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Public webhook (signature-authenticated)
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/clerk").permitAll()
                        // Public read-only catalog endpoints (future)
                        .requestMatchers(HttpMethod.GET, "/api/v1/gyms/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/routes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/exercises/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/workout-templates/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/training-plans/**").permitAll()
                        // Everything else requires a valid Clerk JWT
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(clerkJwtAuthenticationConverter)));
        return http.build();
    }
}
