package com.finfive.crisfin.global.config;

import com.finfive.crisfin.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Stateless JWT utility — issues and validates access/refresh tokens.
 *
 * <p>Configuration keys (application.yml):
 * <pre>
 *   jwt:
 *     secret: &lt;at-least-256-bit key&gt;
 *     access-expiration: 1800000       # 30 min in ms
 *     refresh-expiration: 604800000    # 7 days in ms
 * </pre>
 */
@Slf4j
@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    // ------------------------------------------------------------------ //
    //  Key
    // ------------------------------------------------------------------ //

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ //
    //  Token generation
    // ------------------------------------------------------------------ //

    /**
     * Generates a short-lived access token embedding the user's email and role.
     */
    public String generateAccessToken(String email, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a long-lived refresh token that carries only the subject (email).
     */
    public String generateRefreshToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ------------------------------------------------------------------ //
    //  Validation & extraction
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} when the token is syntactically valid, signed with
     * the correct key and not yet expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the subject (email) from a valid token without checking expiry.
     * Call {@link #validateToken} first if the token may be invalid.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
