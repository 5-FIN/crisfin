package com.finfive.crisfin.global.config;

import com.finfive.crisfin.global.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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
        // 그대로 백엔드에 전달한다. 배포 IP·도메인·포트가 바뀔 때마다 허용 목록을 정확히 맞추는 것은
        // 계속 어긋나 CORS 거절(Invalid CORS request)을 유발했다.
        // 백엔드는 docker 내부 네트워크 전용(호스트로 8080 미노출)이라 CORS는 실질적 보안 경계가
        // 아니므로, 앱 계층에서 모든 Origin을 반사(allow-all)로 고정한다.
        // (allowCredentials=true 에서는 setAllowedOrigins("*")가 금지되므로, 실제 Origin을 그대로
        //  반사해 주는 allowedOriginPatterns("*")를 써야 한다.)
        config.setAllowedOriginPatterns(List.of("*"));
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
