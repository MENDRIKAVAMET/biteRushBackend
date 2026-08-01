package com.biterush.api.dto;

import com.biterush.api.entity.DeliveryStatus;

import java.time.LocalDateTime;

public class DeliveryResponseDTO {

    public Long id;

    public Long orderId;
    public String clientName;
    public String address;
    public String orderPhone;
    public double orderTotal;

    public Long livreurId;
    public String livreurName;

    public DeliveryStatus status;

    public LocalDateTime assignedAt;
    public LocalDateTime deliveredAt;

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}