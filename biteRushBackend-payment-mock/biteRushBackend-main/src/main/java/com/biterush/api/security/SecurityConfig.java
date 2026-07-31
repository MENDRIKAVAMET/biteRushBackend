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

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
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

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html").permitAll()

                        .requestMatchers("/ws/**").permitAll()

                        // --- Produits : lecture publique, écriture admin only ---
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")

                        // --- Restaurants / menu : lecture publique (vitrine) ---
                        .requestMatchers(HttpMethod.GET, "/restaurants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/menu-items/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/restaurants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/restaurants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/restaurants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/menu-items/**").hasAnyRole("ADMIN", "RESTAURANT_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/menu-items/**").hasAnyRole("ADMIN", "RESTAURANT_STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/menu-items/**").hasAnyRole("ADMIN", "RESTAURANT_STAFF")

                        // --- Commandes ---
                        .requestMatchers(HttpMethod.POST, "/orders").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/orders/*/cancel").permitAll()
                        .requestMatchers(HttpMethod.GET, "/orders/my-orders").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/orders/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/orders/**").hasRole("ADMIN")

                        // --- Espace restaurant (staff du resto + admin) ---
                        .requestMatchers("/restaurant/orders/**").hasAnyRole("RESTAURANT_STAFF", "ADMIN")
                        .requestMatchers("/restaurant-staff/**").hasAnyRole("RESTAURANT_STAFF", "ADMIN")

                        // --- Livraisons (livreur + admin) ---
                        .requestMatchers("/api/deliveries/assign").hasAnyRole("RESTAURANT_STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/deliveries/**").hasRole("ADMIN")
                        .requestMatchers("/api/deliveries/**").hasAnyRole("LIVREUR", "ADMIN")

                        // --- Paiement (mock) ---
                        .requestMatchers(HttpMethod.POST, "/payments").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/payments/**").hasAnyRole("CLIENT", "RESTAURANT_STAFF", "ADMIN")
                        .requestMatchers("/payments/*/webhook").hasRole("ADMIN")
                        .requestMatchers("/payments/*/refund").hasRole("ADMIN")
                        .requestMatchers("/payments/*/mark-paid").hasAnyRole("LIVREUR", "RESTAURANT_STAFF", "ADMIN")

                        // --- Admin ---
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // --- Utilisateurs : gestion réservée à l'admin (faille corrigée) ---
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        // --- Notifications : chaque utilisateur ne voit que les siennes (filtré en service) ---
                        .requestMatchers("/notifications/**").authenticated()

                        // --- Adresses, avis, clients, images : authentifié, filtré par propriétaire en service ---
                        .requestMatchers("/addresses/**", "/reviews/**", "/clients/**", "/api/images/**", "/history/**")
                        .authenticated()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}