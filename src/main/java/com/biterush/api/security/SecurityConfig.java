package com.biterush.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;

    public SecurityConfig(JwtFilter jwtFilter, LoginRateLimitFilter loginRateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // Doit être authentifié (précède la règle générale /auth/**
                        // ci-dessous, qui elle est permitAll pour login/register/etc.)
                        .requestMatchers("/auth/change-password").authenticated()
                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/orders").hasRole("CLIENT")

                        .requestMatchers(HttpMethod.PATCH, "/orders/*/cancel").permitAll()

                        .requestMatchers("/orders/*/deliver").hasAnyRole("LIVREUR", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/orders/**").hasAnyRole("CLIENT", "LIVREUR", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/orders/**").hasRole("ADMIN")
                        // PATCH /orders/admin/{id}/deliver ne matchait aucune règle explicite (le
                        // pattern "/orders/*/deliver" ci-dessus ne matche qu'un seul segment, pas
                        // "admin/{id}"), et retombait donc sur anyRequest().authenticated() - accessible
                        // à n'importe quel rôle authentifié côté filtre HTTP. OrderService.markAsDelivered()
                        // appelle validateAdmin() en interne donc aucune brèche réelle, mais la règle
                        // HTTP ne reflétait pas cette exigence. Ajoutée pour que SecurityConfig soit
                        // cohérent avec ce que le service exige réellement.
                        .requestMatchers(HttpMethod.PATCH, "/orders/admin/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/payments/*/webhook").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/payments/*/refund").hasRole("ADMIN")
                        .requestMatchers("/payments/**").hasAnyRole("CLIENT", "ADMIN")

                        .requestMatchers("/restaurant/orders/**").hasAnyRole("RESTAURANT_STAFF", "ADMIN")
                        .requestMatchers("/restaurant-staff/**").hasAnyRole("RESTAURANT_STAFF", "ADMIN")

                        // POST /api/deliveries/assign et GET /api/deliveries : ADMIN uniquement.
                        // DeliveryService.assignDelivery()/.getAllDeliveries() appellent validateAdmin()
                        // en interne (ADMIN strict) - un RESTAURANT_STAFF passerait le filtre HTTP pour
                        // se prendre un 403 côté service si on l'autorisait ici. Voir CHANGELOG pour le
                        // raisonnement complet : le chemin normal pour un staff reste
                        // /restaurant/orders/{id}/assign-delivery (RestaurantService.assignToDelivery,
                        // scopé à son restaurant), donc resserrer ici plutôt que dupliquer le scoping.
                        .requestMatchers(HttpMethod.POST, "/api/deliveries/assign").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/deliveries/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries").hasRole("ADMIN")
                        .requestMatchers("/api/deliveries/**").hasAnyRole("LIVREUR", "ADMIN")

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers("/notifications/**").authenticated()
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        loginRateLimitFilter,
                        JwtFilter.class
                );

        return http.build();
    }
}