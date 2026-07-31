package com.biterush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStats {
    public int pendingCount;
    public int preparingCount;
    public int readyCount;
    public int deliveringCount;
    public int completedCount;
    public int rejectedCount;
    public int cancelledCount;
    public double totalRevenue;
    public int totalOrders;
}
