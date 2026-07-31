package com.biterush.api.dto;

import com.biterush.api.entity.OrderStatus;
import jakarta.validation.Valid;

import java.util.List;

public class OrderUpdateDTO {
    public String clientName;
    public String phone;
    public String address;

    public OrderStatus status;

    @Valid
    public List<OrderItemDTO> items;
}
