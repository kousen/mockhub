package com.mockhub.notification.dto;

import java.time.Instant;

import com.mockhub.notification.entity.NotificationType;

public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean isRead,
        Instant createdAt
) {
}
