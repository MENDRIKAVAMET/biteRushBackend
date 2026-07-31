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

                        .requestMatchers(HttpMethod.POST, "/payments/*/webhook").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/payments/*/refund").hasRole("ADMIN")
                        .requestMatchers("/payments/**").hasAnyRole("CLIENT", "ADMIN")

                        .requestMatchers("/restaurant/orders/**").hasAnyRole("RESTAURANT_STAFF", "ADMIN")
                        .requestMatchers("/restaurant-staff/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/deliveries/assign").hasAnyRole("RESTAURANT_STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/deliveries/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries").hasAnyRole("RESTAURANT_STAFF", "ADMIN")
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
                );

        return http.build();
    }
}