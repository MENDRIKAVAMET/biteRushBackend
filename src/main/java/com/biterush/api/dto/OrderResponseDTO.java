package com.biterush.api.dto;

import com.biterush.api.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDTO {
    public Long id;
    public String clientName;
    public String phone;
    public String address;
    public double total;
    public double deliveryFee;
    public Long restaurantId;
    public OrderStatus status;
    public LocalDateTime createAt;
    public List<OrderItemResponseDTO> items;
}
