package com.mockhub.notification.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.favorite.entity.Favorite;
import com.mockhub.favorite.repository.FavoriteRepository;
import com.mockhub.notification.dto.NotificationDto;
import com.mockhub.notification.entity.Notification;
import com.mockhub.notification.entity.NotificationType;
import com.mockhub.notification.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final FavoriteRepository favoriteRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               EventRepository eventRepository,
                               FavoriteRepository favoriteRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional
    public void createNotification(Long userId, NotificationType type, String title, String message, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notificationRepository.save(notification);
        log.info("Created {} notification for user {}", type, userId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getUserNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<NotificationDto> content = notificationPage.getContent().stream()
                .map(this::toNotificationDto)
                .toList();

        return new PagedResponse<>(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have access to this notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
            log.debug("Marked notification {} as read for user {}", notificationId, user.getId());
        }
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByUserId(user.getId());
        log.info("Marked all notifications as read for user {}", user.getId());
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void sendEventReminders() {
        Instant now = Instant.now();
        Instant twentyFourHoursFromNow = now.plus(24, ChronoUnit.HOURS);

        // Find active events happening in the next 24 hours
        List<Event> upcomingEvents = eventRepository.findAll().stream()
                .filter(event -> "ACTIVE".equals(event.getStatus()))
                .filter(event -> event.getEventDate().isAfter(now))
                .filter(event -> event.getEventDate().isBefore(twentyFourHoursFromNow))
                .toList();

        if (upcomingEvents.isEmpty()) {
            return;
        }

        List<Long> eventIds = upcomingEvents.stream()
                .map(Event::getId)
                .toList();

        List<Favorite> favorites = favoriteRepository.findByEventIdIn(eventIds);

        for (Favorite favorite : favorites) {
            Event event = favorite.getEvent();
            String title = "Event Reminder";
            String message = String.format("'%s' is happening soon! Don't forget to get your tickets.",
                    event.getName());
            String link = "/events/" + event.getSlug();

            createNotification(
                    favorite.getUser().getId(),
                    NotificationType.EVENT_REMINDER,
                    title,
                    message,
                    link
            );
        }

        log.info("Sent {} event reminder notifications for {} upcoming events",
                favorites.size(), upcomingEvents.size());
    }

    private NotificationDto toNotificationDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
