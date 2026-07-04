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

    /**
     * 허용할 Origin 목록 (콤마 구분). {@code cors.allowed-origins} → {@code CORS_ALLOWED_ORIGINS} env로 덮어쓴다.
     * 정확한 Origin(http://localhost:3000)뿐 아니라 패턴(https://*.vercel.app)도 그대로 지원한다.
     */
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

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
        // 허용 Origin은 application.yml의 cors.allowed-origins(=CORS_ALLOWED_ORIGINS env)에서 주입한다.
        // setAllowedOrigins 대신 allowedOriginPatterns를 쓰는 이유:
        //   1) allowCredentials=true 에서 정확 매칭/패턴("https://*.vercel.app") 모두 안전하게 지원.
        //   2) 배포 포트·서브도메인이 바뀌어도 패턴 한 줄로 흡수 가능.
        // 배포 프론트 도메인이 바뀌면 CORS_ALLOWED_ORIGINS 값에 추가하면 된다.
        config.setAllowedOriginPatterns(allowedOrigins);
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
