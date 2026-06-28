<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# config

## Purpose
Spring configuration beans for cross-cutting concerns. Centralizes security policy (JWT filter chain), CORS settings, OpenAPI documentation configuration, JPA/QueryDSL setup, and any other infrastructure wiring that doesn't belong in a feature package.

## For AI Agents

### Working In This Directory
- Each configuration concern gets its own `@Configuration` class — do not put everything in one `AppConfig`
- Security config must use the `SecurityFilterChain` bean pattern (Spring Security 6+, not the deprecated `WebSecurityConfigurerAdapter`)
- JWT filter should be a `OncePerRequestFilter` registered in the filter chain before `UsernamePasswordAuthenticationFilter`
- Public endpoints (Swagger, health check, auth) must be explicitly permitted in the security config

### Common Patterns
```java
// Spring Security 6 pattern
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### Expected Config Classes
| Class | Purpose |
|-------|---------|
| `SecurityConfig` | JWT filter chain, CORS, CSRF disable |
| `JwtConfig` or `JwtProvider` | Token generation and validation (uses jjwt 0.12.6) |
| `SwaggerConfig` | OpenAPI 3 bean customization |
| `QueryDslConfig` | `JPAQueryFactory` bean |

<!-- MANUAL: -->
