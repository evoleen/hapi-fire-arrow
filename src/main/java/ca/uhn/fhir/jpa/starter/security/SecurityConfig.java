package ca.uhn.fhir.jpa.starter.security;

import ca.uhn.fhir.jpa.starter.config.AuthConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security configuration for OAuth 2.0 JWT authentication.
 * Configures security filter chains for FHIR endpoints with JWT token validation.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "hapi.fhir.auth.enabled", havingValue = "true")
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private AuthConfigurationProperties authConfig;

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Security filter chain for FHIR REST endpoints.
     * Configures OAuth2 Resource Server with JWT validation.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain fhirSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring OAuth2 JWT security for FHIR endpoints");
        
        http
            .securityMatcher("/fhir/**")
            .authorizeHttpRequests(authz -> authz
                // Allow metadata endpoint for FHIR capability statement
                .requestMatchers("/fhir/metadata").permitAll()
                // Allow health check endpoints
                .requestMatchers("/fhir/_health").permitAll()
                // Require authentication for all other FHIR endpoints
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            );

        return http.build();
    }

    /**
     * Security filter chain for management and admin endpoints.
     * These endpoints bypass OAuth authentication but may have other security measures.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security for management endpoints");
        
        http
            .securityMatcher("/actuator/**", "/admin/**")
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .anyRequest().denyAll() // Restrict other actuator endpoints
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Default security filter chain for all other endpoints.
     * Allows public access to web UI and test pages.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring default security filter chain");
        
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/home", "/about", "/css/**", "/js/**", "/img/**").permitAll()
                .anyRequest().permitAll() // Allow access to HAPI FHIR test page and other resources
            )
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * JWT Decoder bean - configured by OAuth2ResourceServerConfig
     */
    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        // This will be configured by OAuth2ResourceServerConfig based on auth providers
        return new OAuth2ResourceServerConfig(authConfig).jwtDecoder();
    }

    /**
     * JWT Authentication Converter to extract user authorities from JWT claims
     */
    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new OAuth2ResourceServerConfig(authConfig).jwtAuthenticationConverter();
    }

    /**
     * CORS configuration to allow cross-origin requests from web clients
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS for OAuth2 authentication");
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // Configure appropriately for production
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "ETag", 
            "Last-Modified",
            "Location"
        ));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/fhir/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        
        return source;
    }
}
