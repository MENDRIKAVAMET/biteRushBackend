package com.biterush.api.controller;

import com.biterush.api.dto.NotificationDTO;
import com.biterush.api.entity.User;
import com.biterush.api.security.BusinessSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * WebSocket message handler for real-time notifications
 * Handles STOMP messages for order and delivery updates
 */
@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class WebSocketMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final BusinessSecurityUtil businessSecurity;

    /**
     * Handle notification messages sent by clients
     * Broadcasts to subscribed clients
     */
    @MessageMapping("/notify/{userId}")
    @SendTo("/topic/user/{userId}")
    public NotificationDTO sendUserNotification(
            @DestinationVariable String userId,
            NotificationDTO notification
    ) {
        User currentUser = businessSecurity.getCurrentUser();
        
        // Users can only send notifications to themselves
        if (!currentUser.getId().toString().equals(userId)) {
            throw new SecurityException("Vous ne pouvez envoyer des notifications qu'à votre compte");
        }
        
        return notification;
    }

    /**
     * Handle delivery status updates
     * Only delivery persons can send to delivery topic
     */
    @MessageMapping("/delivery/{deliveryId}/update")
    @SendTo("/topic/delivery/{deliveryId}")
    public NotificationDTO sendDeliveryUpdate(
            @DestinationVariable String deliveryId,
            NotificationDTO notification
    ) {
        User currentUser = businessSecurity.getCurrentUser();
        businessSecurity.requireDeliveryPerson();
        
        return notification;
    }

    /**
     * Handle restaurant order updates
     * Only restaurant staff can send order notifications
     */
    @MessageMapping("/restaurant/{restaurantId}/order")
    @SendTo("/topic/restaurant/{restaurantId}")
    public NotificationDTO sendRestaurantOrderUpdate(
            @DestinationVariable String restaurantId,
            NotificationDTO notification
    ) {
        User currentUser = businessSecurity.getCurrentUser();
        
        // Only admins or restaurant staff can send
        if (!businessSecurity.isAdmin(currentUser)) {
            throw new SecurityException("Accès refusé à ce canal");
        }
        
        return notification;
    }

    /**
     * Admin broadcast to all clients
     */
    @MessageMapping("/admin/broadcast")
    @SendTo("/topic/admin")
    public NotificationDTO adminBroadcast(NotificationDTO notification) {
        User currentUser = businessSecurity.getCurrentUser();
        businessSecurity.requireAdmin();
        
        return notification;
    }

    /**
     * Send notification to specific user (server-side)
     */
    public void notifyUser(Long userId, NotificationDTO notification) {
        messagingTemplate.convertAndSend(
                "/topic/user/" + userId,
                notification
        );
    }

    /**
     * Send notification to specific delivery person
     */
    public void notifyDeliveryPerson(Long deliveryId, NotificationDTO notification) {
        messagingTemplate.convertAndSend(
                "/topic/delivery/" + deliveryId,
                notification
        );
    }

    /**
     * Send notification to specific restaurant
     */
    public void notifyRestaurant(Long restaurantId, NotificationDTO notification) {
        messagingTemplate.convertAndSend(
                "/topic/restaurant/" + restaurantId,
                notification
        );
    }

    /**
     * Broadcast notification to all connected users
     */
    public void broadcastNotification(NotificationDTO notification) {
        messagingTemplate.convertAndSend(
                "/topic/notifications",
                notification
        );
    }

    /**
     * Broadcast order update to all users
     */
    public void broadcastOrderUpdate(NotificationDTO notification) {
        messagingTemplate.convertAndSend(
                "/topic/orders",
                notification
        );
    }
}
