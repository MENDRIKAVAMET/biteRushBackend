package com.biterush.api.security;

import com.biterush.api.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;

    private final long expirationMs = 1000 * 60 * 60; // 1h
    private final long refreshExpirationMs = 1000 * 60 * 60 * 24 * 7; // 7 days

    public JwtService(
            @Value("${jwt.secret}") String secret
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // =========================
    // GENERATION TOKEN
    // =========================
    public String generateToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("roles", List.of(user.getRole())) // évolutif
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(key)
                .compact();
    }

    // =========================
    // EXTRACTION CLAIMS
    // =========================
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {

        return extractAllClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        return extractEmail(token);
    }

    public Long extractUserId(String token) {

        return extractAllClaims(token).get("userId", Long.class);
    }

    public List<String> extractRoles(String token) {

        return extractAllClaims(token).get("roles", List.class);
    }

    public boolean isTokenValid(String token) {

        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, User user) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject().equals(user.getEmail()) && 
                   claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}