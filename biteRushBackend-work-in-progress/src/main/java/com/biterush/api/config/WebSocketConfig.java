package com.biterush.api.config;

import com.biterush.api.security.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            if (!jwtService.isTokenValid(token)) {
                                throw new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED, "Token expiré ou invalide"
                                );
                            }

                            String email = jwtService.extractEmail(token);
                            List<String> roles = jwtService.extractRoles(token);

                            List<SimpleGrantedAuthority> authorities = roles.stream()
                                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                    .toList();

                            UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                    email, null, authorities
                                );
                            accessor.setUser(auth);

                        } catch (ResponseStatusException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED, "Token WebSocket invalide"
                            );
                        }
                    } else {
                        throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Authentification WebSocket requise"
                        );
                    }
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Authentication auth = (Authentication) accessor.getUser();
                    if (auth == null || !auth.isAuthenticated()) {
                        throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Authentification requise pour souscrire"
                        );
                    }
                    validateSubscription(accessor.getDestination(), auth);
                }

                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    Authentication auth = (Authentication) accessor.getUser();
                    if (auth == null || !auth.isAuthenticated()) {
                        throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Authentification requise"
                        );
                    }
                }

                return message;
            }
        });
    }

    private void validateSubscription(String destination, Authentication auth) {
        if (destination == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination manquante");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        if (destination.startsWith("/topic/user/") ||
            destination.startsWith("/topic/orders") ||
            destination.startsWith("/topic/notifications") ||
            destination.startsWith("/user/queue/")) {
            return;
        }

        if (destination.startsWith("/topic/delivery/")) {
            boolean isLivreur = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LIVREUR"));
            if (!isLivreur) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à ce topic");
            }
            return;
        }

        if (destination.startsWith("/topic/restaurant/")) return;

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé: " + destination);
    }
}