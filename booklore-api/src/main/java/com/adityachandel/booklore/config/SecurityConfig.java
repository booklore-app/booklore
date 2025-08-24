package com.adityachandel.booklore.config;

import com.adityachandel.booklore.config.properties.AppProperties;
import com.adityachandel.booklore.config.security.CustomOpdsUserDetailsService;
import com.adityachandel.booklore.config.security.filters.DualJwtAuthenticationFilter;
import com.adityachandel.booklore.config.security.filters.KoboAuthFilter;
import com.adityachandel.booklore.config.security.filters.KoreaderAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private final AppProperties appProperties;
    private final CustomOpdsUserDetailsService customOpdsUserDetailsService;

    // =====================
    // Swagger / API Docs
    // =====================
    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    // =====================
    // Common public endpoints
    // Requests matching these paths should always be accessible without authentication.
    // =====================
    private static final String[] COMMON_PUBLIC_ENDPOINTS = {
            "/ws/**",
            "/api/v1/auth/**",
            "/api/v1/public-settings",
            "/api/v1/setup/**"
    };

    // =====================
    // Cover / file endpoints
    // Publicly accessible endpoints for book assets (cover images, CBX/PDF pages)
    // These do not require authentication.
    // =====================
    private static final String[] COVER_PUBLIC_ENDPOINTS = {
            "/api/v1/books/*/cover",
            "/api/v1/books/*/backup-cover",
            "/api/v1/opds/*/cover.jpg",
            "/api/v1/cbx/*/pages/*",
            "/api/v1/pdf/*/pages/*",
            "/api/v1/bookdrop/*/cover"
    };

    // =====================
    // OPDS unauthenticated endpoints
    // =====================
    private static final String[] COMMON_UNAUTHENTICATED_ENDPOINTS = {
            "/api/v1/opds/search.opds"
    };

    // =====================
    // Password Encoder
    // =====================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // =====================
    // OPDS Basic Auth Chain
    // =====================
    @Bean
    @Order(1)
    public SecurityFilterChain opdsBasicAuthSecurityChain(HttpSecurity http) throws Exception {

        List<String> unauthenticatedEndpoints = new ArrayList<>(Arrays.asList(COMMON_UNAUTHENTICATED_ENDPOINTS));
        http
                .securityMatcher("/api/v1/opds/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(unauthenticatedEndpoints.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic
                        .realmName("Booklore OPDS")
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setHeader("WWW-Authenticate", "Basic realm=\"Booklore OPDS\"");
                            response.getWriter().write("HTTP Status 401 - " + authException.getMessage());
                        })
                );
        return http.build();
    }

    // =====================
    // Koreader Security Chain
    // =====================
    @Bean
    @Order(2)
    public SecurityFilterChain koreaderSecurityChain(HttpSecurity http, KoreaderAuthFilter koreaderAuthFilter) throws Exception {
        http.securityMatcher("/api/koreader/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(koreaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // =====================
    // Kobo Security Chain
    // =====================
    @Bean
    @Order(3)
    public SecurityFilterChain koboSecurityChain(HttpSecurity http, KoboAuthFilter koboAuthFilter) throws Exception {
        http.securityMatcher("/api/kobo/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(koboAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // =====================
    // JWT API Security Chain (Final Boss Chain)
    // =====================
    @Bean
    @Order(4)
    public SecurityFilterChain jwtApiSecurityChain(HttpSecurity http, DualJwtAuthenticationFilter dualJwtAuthenticationFilter) throws Exception {
        List<String> publicEndpoints = new ArrayList<>(Arrays.asList(COMMON_PUBLIC_ENDPOINTS));
        publicEndpoints.addAll(Arrays.asList(COVER_PUBLIC_ENDPOINTS));
        if (appProperties.getSwagger().isEnabled()) {
            publicEndpoints.addAll(Arrays.asList(SWAGGER_ENDPOINTS));
        }
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpoints.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(dualJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =====================
    // Authentication Manager & Provider for OPDS
    // =====================
    /**
     * AuthenticationManager bean is required because we’re explicitly using
     * username/password (Basic Auth) for OPDS instead of JWT/OIDC.
     *
     * It delegates to the AuthenticationProvider, which in turn uses our
     * CustomOpdsUserDetailsService for looking up OPDS users and verifying credentials.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authenticationProvider())
                .build();
    }

    /**
     * DaoAuthenticationProvider configured with:
     * - customOpdsUserDetailsService → loads OPDS-specific users
     * - passwordEncoder → ensures passwords are properly hashed/verified
     *
     * This is specifically scoped for OPDS Basic Authentication and
     * should not interfere with JWT/OIDC chains.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customOpdsUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // =====================
    // CORS Config
    // =====================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}