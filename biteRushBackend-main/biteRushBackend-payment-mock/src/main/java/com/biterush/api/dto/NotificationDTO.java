package com.biterush.api.dto;

import com.biterush.api.entity.NotificationType;
import java.time.LocalDateTime;

public class NotificationDTO {
    public Long id;
    public NotificationType type;
    public String message;
    public Long targetEntityId;
    public boolean read;
    public LocalDateTime createdAt;
}
