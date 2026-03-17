package com.mockhub.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.favorite.repository.FavoriteRepository;
import com.mockhub.notification.dto.NotificationDto;
import com.mockhub.notification.entity.Notification;
import com.mockhub.notification.entity.NotificationType;
import com.mockhub.notification.repository.NotificationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private User otherUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setRoles(Set.of(buyerRole));

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setRoles(Set.of(buyerRole));

        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setType(NotificationType.ORDER_CONFIRMED);
        testNotification.setTitle("Order Confirmed");
        testNotification.setMessage("Your order has been confirmed.");
        testNotification.setLink("/orders/MH-123");
        testNotification.setRead(false);
        testNotification.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("createNotification - given valid user - saves notification")
    void createNotification_givenValidUser_savesNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        notificationService.createNotification(
                1L, NotificationType.ORDER_CONFIRMED,
                "Test Title", "Test message", "/test");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createNotification - given unknown user - throws ResourceNotFoundException")
    void createNotification_givenUnknownUser_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.createNotification(
                        999L, NotificationType.SYSTEM,
                        "Title", "Message", null),
                "Should throw ResourceNotFoundException for unknown user");
    }

    @Test
    @DisplayName("getUserNotifications - given user with notifications - returns paged response")
    void getUserNotifications_givenUserWithNotifications_returnsPagedResponse() {
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<NotificationDto> result = notificationService.getUserNotifications(testUser, 0, 20);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one notification");
        assertEquals("Order Confirmed", result.content().get(0).title(), "Title should match");
    }

    @Test
    @DisplayName("getUnreadCount - given unread notifications - returns count")
    void getUnreadCount_givenUnreadNotifications_returnsCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(testUser);

        assertEquals(5L, count, "Unread count should be 5");
    }

    @Test
    @DisplayName("markAsRead - given own unread notification - marks it as read")
    void markAsRead_givenOwnUnreadNotification_marksItAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(testUser, 1L);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead - given already read notification - does not save again")
    void markAsRead_givenAlreadyReadNotification_doesNotSaveAgain() {
        testNotification.setRead(true);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(testUser, 1L);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead - given other user's notification - throws UnauthorizedException")
    void markAsRead_givenOtherUsersNotification_throwsUnauthorizedException() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        assertThrows(UnauthorizedException.class,
                () -> notificationService.markAsRead(otherUser, 1L),
                "Should throw UnauthorizedException for other user's notification");
    }

    @Test
    @DisplayName("markAsRead - given nonexistent notification - throws ResourceNotFoundException")
    void markAsRead_givenNonexistentNotification_throwsResourceNotFoundException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(testUser, 999L),
                "Should throw ResourceNotFoundException for unknown notification");
    }

    @Test
    @DisplayName("markAllAsRead - given user - delegates to repository")
    void markAllAsRead_givenUser_delegatesToRepository() {
        notificationService.markAllAsRead(testUser);

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }
}
