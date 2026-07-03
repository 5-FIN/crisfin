package com.finfive.crisfin.global.config;

import com.finfive.crisfin.global.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <ul>
 *   <li>CSRF disabled — stateless JWT API</li>
 *   <li>Session management set to STATELESS</li>
 *   <li>Public paths: auth, crisis, guide, welfare, analysis, OpenAPI/Swagger</li>
 *   <li>Every other request requires a valid JWT</li>
 *   <li>{@code @PreAuthorize} / {@code @PostAuthorize} enabled via {@code @EnableMethodSecurity}</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Value("${cors.allowed-origins:*}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/crisis/**",
                                "/api/v1/guide/**",
                                "/api/v1/welfare/**",
                                "/api/v1/analysis/**",
                                "/api/v1/mydata/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userDetailsService);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 프론트(Next.js)가 /api/*를 서버사이드로 프록시하지만, 프록시는 브라우저의 Origin 헤더를
        // 그대로 백엔드에 전달한다. 따라서 배포 IP·도메인·localhost·127.0.0.1 등 어떤 Origin으로
        // 접속하든 매칭되도록 allowedOriginPatterns를 사용한다.
        // (allowCredentials=true 에서는 setAllowedOrigins("*")가 금지되므로, 실제 Origin을 그대로
        //  반사해 주는 allowedOriginPatterns를 써야 한다.)
        // 공백·빈 값·후행 슬래시를 정규화한다. (예: "https://a.com, https://b.com/" -> [...])
        List<String> allowedOriginPatterns = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(origin -> origin.endsWith("/")
                        ? origin.substring(0, origin.length() - 1)
                        : origin)
                .toList();
        config.setAllowedOriginPatterns(
                allowedOriginPatterns.isEmpty() ? List.of("*") : allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
