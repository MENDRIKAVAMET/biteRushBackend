package com.biterush.api.controller;

import com.biterush.api.dto.NotificationDTO;
import com.biterush.api.dto.SubscriptionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("http://localhost:5173")
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/notifications/subscribe")
    public void subscribe(SubscriptionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = getCurrentUserId();
            String topic = message.getTopic();

            validateSubscription(userId, topic);

            String sessionId = headerAccessor.getSessionId();
            log.info("User {} subscribed to topic: {} (Session: {})", userId, topic, sessionId);

            SubscriptionMessage response = new SubscriptionMessage();
            response.setTopic(topic);
            response.setMessage("Successfully subscribed to " + topic);
            response.setSuccess(true);

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/subscription-confirmation",
                    response
            );
        } catch (IllegalArgumentException e) {
            log.warn("Subscription validation failed: {}", e.getMessage());
            SubscriptionMessage response = new SubscriptionMessage();
            response.setMessage("Subscription failed: " + e.getMessage());
            response.setSuccess(false);
            messagingTemplate.convertAndSendToUser(
                    getCurrentUserId().toString(),
                    "/queue/subscription-confirmation",
                    response
            );
        }
    }

    @MessageMapping("/notifications/unsubscribe")
    public void unsubscribe(SubscriptionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = getCurrentUserId();
            String topic = message.getTopic();

            validateSubscription(userId, topic);

            String sessionId = headerAccessor.getSessionId();
            log.info("User {} unsubscribed from topic: {} (Session: {})", userId, topic, sessionId);

            SubscriptionMessage response = new SubscriptionMessage();
            response.setTopic(topic);
            response.setMessage("Successfully unsubscribed from " + topic);
            response.setSuccess(true);

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/unsubscription-confirmation",
                    response
            );
        } catch (IllegalArgumentException e) {
            log.warn("Unsubscription validation failed: {}", e.getMessage());
        }
    }

    private void validateSubscription(Long userId, String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }

        if (topic.startsWith("/topic/notifications/")) {
            String recipientIdStr = topic.substring("/topic/notifications/".length());
            try {
                Long recipientId = Long.parseLong(recipientIdStr);
                if (!recipientId.equals(userId)) {
                    throw new IllegalArgumentException(
                            "You can only subscribe to your own notifications"
                    );
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid recipient ID in topic");
            }
        } else if (topic.startsWith("/topic/deliveries/")) {
            String deliveryPersonIdStr = topic.substring("/topic/deliveries/".length());
            try {
                Long deliveryPersonId = Long.parseLong(deliveryPersonIdStr);
                if (!deliveryPersonId.equals(userId)) {
                    throw new IllegalArgumentException(
                            "You can only subscribe to your own delivery notifications"
                    );
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid delivery person ID in topic");
            }
        } else if (topic.equals("/topic/orders/dashboard")) {
            log.debug("Allowing dashboard subscription for user: {}", userId);
        } else {
            throw new IllegalArgumentException("Invalid topic: " + topic);
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.biterush.api.entity.User user) {
            return user.getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
