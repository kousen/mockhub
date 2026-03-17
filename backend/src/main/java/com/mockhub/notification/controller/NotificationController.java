package com.mockhub.notification.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.notification.dto.NotificationDto;
import com.mockhub.notification.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationDto>> listNotifications(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = resolveUser(securityUser);
        return ResponseEntity.ok(notificationService.getUserNotifications(user, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser securityUser) {
        User user = resolveUser(securityUser);
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long id) {
        User user = resolveUser(securityUser);
        notificationService.markAsRead(user, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal SecurityUser securityUser) {
        User user = resolveUser(securityUser);
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(SecurityUser securityUser) {
        return userRepository.findByEmail(securityUser.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", securityUser.getEmail()));
    }
}
