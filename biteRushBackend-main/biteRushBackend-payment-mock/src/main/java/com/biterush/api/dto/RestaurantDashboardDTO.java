package com.biterush.api.dto;

import java.util.List;

public class RestaurantDashboardDTO {
    public int pendingCount;
    public int preparingCount;
    public int readyCount;
    public List<OrderResponseDTO> pending;
    public List<OrderResponseDTO> preparing;
    public List<OrderResponseDTO> ready;
}
