package com.biterush.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting sur POST /auth/login uniquement (mission : "Rate limiting sur
 * /auth/login"). Intervient avant JwtFilter/le contrôleur - une requête bloquée ici
 * ne consomme même pas de connexion base de données côté AuthService.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimiter rateLimiter;

    public LoginRateLimitFilter(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);

        if (!rateLimiter.tryAcquire(clientKey)) {
            long retryAfterSeconds = rateLimiter.secondsUntilRetry(clientKey);

            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Trop de tentatives de connexion, réessayez plus tard.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith("/auth/login");
    }

    /**
     * Résout l'IP cliente. Vérifie d'abord X-Forwarded-For (cas d'un reverse proxy /
     * load balancer devant l'application), sinon retombe sur l'IP de connexion
     * directe. Ne prend que la première IP de la liste X-Forwarded-For (celle du
     * client d'origine).
     */
    private String resolveClientKey(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
