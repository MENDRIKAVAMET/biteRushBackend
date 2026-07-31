package com.biterush.api.service;

import com.biterush.api.dto.NotificationDTO;
import com.biterush.api.entity.*;
import com.biterush.api.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /*
     * =========================================================
     * CRÉER NOTIFICATIONS
     * =========================================================
     */

    public void notifyOrderCreated(Order order) {
        String message = "Nouvelle commande #" + order.getId() + " reçue";
        createNotification(order.getAssignedTo().getUser(), 
                          NotificationType.ORDER_CREATED,
                          order.getId(), message);
    }

    public void notifyOrderStatusChanged(Order order, OrderStatus newStatus) {
        String message = "Commande #" + order.getId() + " - " +
                        getStatusFrench(newStatus);
        
        NotificationType type = mapStatusToNotificationType(newStatus);
        
        createNotification(order.getCreator(), type, order.getId(), message);
    }

    public void notifyOrderCancelled(Order order) {
        String message = "Commande #" + order.getId() + " annulée par le client";
        
        createNotification(order.getAssignedTo().getUser(),
                          NotificationType.ORDER_CANCELLED,
                          order.getId(), message);
    }

    public void notifyDeliveryAssigned(Delivery delivery) {
        String message = "Nouvelle livraison assignée - Commande #" +
                        delivery.getOrder().getId();
        
        createNotification(delivery.getLivreur(),
                          NotificationType.DELIVERY_ASSIGNED,
                          delivery.getId(), message);
    }

    public void notifyDeliveryStatusChanged(Delivery delivery, DeliveryStatus newStatus) {
        String message = "Livraison #" + delivery.getId() + " - " +
                        getDeliveryStatusFrench(newStatus);
        
        NotificationType type = mapDeliveryStatusToNotificationType(newStatus);
        
        createNotification(delivery.getOrder().getCreator(),
                          type, delivery.getId(), message);
    }

    private void createNotification(User recipient, NotificationType type,
                                   Long targetEntityId, String message) {
        
        if (recipient == null) {
            return;
        }
        
        Notification notification = new Notification(recipient, type,
                                                     targetEntityId, message);
        notificationRepository.save(notification);
        
        // Broadcast via WebSocket
        broadcastNotification(notification);
    }

    private void broadcastNotification(Notification notification) {
        try {
            NotificationDTO dto = new NotificationDTO();
            dto.id = notification.getId();
            dto.type = notification.getType();
            dto.message = notification.getMessage();
            dto.read = notification.isRead();
            dto.targetEntityId = notification.getTargetEntityId();
            dto.createdAt = notification.getCreatedAt();
            
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.getRecipient().getId(),
                dto
            );
        } catch (Exception e) {
            // Log but don't fail if WebSocket broadcast fails
            System.err.println("Failed to broadcast notification: " + e.getMessage());
        }
    }

    /*
     * =========================================================
     * LIRE NOTIFICATIONS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(Long userId) {

        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(Long userId) {

        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {

        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    /*
     * =========================================================
     * GÉRER NOTIFICATIONS
     * =========================================================
     */

    /**
     * Récupère l'entité Notification (usage interne, pour vérification de
     * propriétaire côté contrôleur avant markAsRead/delete).
     */
    @Transactional(readOnly = true)
    public Notification getNotificationEntity(Long notificationId) {

        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification introuvable"
                ));
    }

    public void markAsRead(Long notificationId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification introuvable"
                ));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {

        notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    public void deleteNotification(Long notificationId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification introuvable"
                ));

        notificationRepository.delete(notification);
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private NotificationType mapStatusToNotificationType(OrderStatus status) {
        return switch (status) {
            case EN_ATTENTE -> NotificationType.ORDER_CREATED;
            case CONFIRMEE -> NotificationType.ORDER_ACCEPTED;
            case REJETEE -> NotificationType.ORDER_CANCELLED;
            case EN_PREPARATION -> NotificationType.ORDER_IN_PREPARATION;
            case PRETE -> NotificationType.ORDER_READY;
            case EN_LIVRAISON -> NotificationType.ORDER_IN_PREPARATION;
            case LIVREE -> NotificationType.ORDER_DELIVERED;
            case ANNULEE -> NotificationType.ORDER_CANCELLED;
        };
    }

    private NotificationType mapDeliveryStatusToNotificationType(DeliveryStatus status) {
        return switch (status) {
            case CREATED -> NotificationType.DELIVERY_ASSIGNED;
            case ASSIGNED -> NotificationType.DELIVERY_ASSIGNED;
            case IN_PROGRESS -> NotificationType.DELIVERY_IN_PROGRESS;
            case DELIVERED -> NotificationType.DELIVERY_COMPLETED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
        };
    }

    private String getStatusFrench(OrderStatus status) {
        return switch (status) {
            case EN_ATTENTE -> "En attente";
            case CONFIRMEE -> "Confirmée";
            case REJETEE -> "Rejetée";
            case EN_PREPARATION -> "En préparation";
            case PRETE -> "Prête";
            case EN_LIVRAISON -> "En livraison";
            case LIVREE -> "Livrée";
            case ANNULEE -> "Annulée";
        };
    }

    private String getDeliveryStatusFrench(DeliveryStatus status) {
        return switch (status) {
            case CREATED -> "Créée";
            case ASSIGNED -> "Assignée";
            case IN_PROGRESS -> "En cours";
            case DELIVERED -> "Livrée";
            case CANCELLED -> "Annulée";
        };
    }

    /*
     * =========================================================
     * DTO MAPPING
     * =========================================================
     */

    private NotificationDTO mapToDTO(Notification notification) {

        NotificationDTO dto = new NotificationDTO();

        dto.id = notification.getId();
        dto.type = notification.getType();
        dto.message = notification.getMessage();
        dto.targetEntityId = notification.getTargetEntityId();
        dto.read = notification.isRead();
        dto.createdAt = notification.getCreatedAt();

        return dto;
    }
}
