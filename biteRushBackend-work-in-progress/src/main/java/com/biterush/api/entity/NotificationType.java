package com.biterush.api.entity;

public enum NotificationType {
    ORDER_CREATED,           // Responsable: nouvelle commande reçue
    ORDER_ACCEPTED,          // Client: commande acceptée par restaurant
    ORDER_IN_PREPARATION,    // Client: en préparation
    ORDER_READY,             // Restaurant: prête pour livraison
    ORDER_CANCELLED,         // Restaurant: annulée par client
    ORDER_DELIVERED,         // Client: livrée
    
    DELIVERY_ASSIGNED,       // Livreur: nouvelle livraison assignée
    DELIVERY_ACCEPTED,       // Restaurant: livreur a accepté
    DELIVERY_IN_PROGRESS,    // Client: en route
    DELIVERY_COMPLETED       // Client: livrée avec succès
}
