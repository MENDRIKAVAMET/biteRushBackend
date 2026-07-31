package com.biterush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    public Object stats;
    public Object recentOrders;
    public Object topMetrics;
}
