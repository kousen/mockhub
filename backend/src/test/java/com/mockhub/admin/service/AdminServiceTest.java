package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.mockhub.admin.dto.AdminEventDto;
import com.mockhub.admin.dto.DashboardStatsDto;
import com.mockhub.auth.dto.UserDto;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.service.EventService;
import com.mockhub.order.entity.Order;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private AdminService adminService;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("admin@example.com");
        testUser.setFirstName("Admin");
        testUser.setLastName("User");
        testUser.setRoles(Set.of(buyerRole));
        testUser.setCreatedAt(Instant.now());

        Venue testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Test Venue");
        testVenue.setSlug("test-venue");
        testVenue.setCity("New York");
        testVenue.setState("NY");
        testVenue.setVenueType("ARENA");
        testVenue.setCapacity(20000);
        testVenue.setSections(new java.util.ArrayList<>());

        Category testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Concert");
        testCategory.setSlug("concert");
        testCategory.setIcon("music");
        testCategory.setSortOrder(1);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");
        testEvent.setDescription("Description");
        testEvent.setArtistName("Artist");
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        testEvent.setStatus("ACTIVE");
        testEvent.setBasePrice(new BigDecimal("50.00"));
        testEvent.setMinPrice(new BigDecimal("50.00"));
        testEvent.setMaxPrice(new BigDecimal("100.00"));
        testEvent.setTotalTickets(1000);
        testEvent.setAvailableTickets(800);
        testEvent.setFeatured(false);
        testEvent.setVenue(testVenue);
        testEvent.setCategory(testCategory);
        testEvent.setTags(new HashSet<>());
    }

    @Test
    @DisplayName("getDashboardStats - given data exists - returns stats DTO")
    void getDashboardStats_givenDataExists_returnsStatsDto() {
        when(userRepository.count()).thenReturn(100L);
        when(orderRepository.count()).thenReturn(50L);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(eventRepository.findAll()).thenReturn(List.of(testEvent));
        when(listingRepository.count()).thenReturn(200L);
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        DashboardStatsDto result = adminService.getDashboardStats();

        assertNotNull(result, "Dashboard stats should not be null");
        assertEquals(100L, result.totalUsers(), "Total users should match");
        assertEquals(50L, result.totalOrders(), "Total orders should match");
        assertEquals(1L, result.activeEvents(), "Active events should be 1");
        assertEquals(200L, result.totalListings(), "Total listings should match");
    }

    @Test
    @DisplayName("getAllEvents - given events exist - returns paged admin event DTOs")
    void getAllEvents_givenEventsExist_returnsPagedAdminEventDtos() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> page = new PageImpl<>(List.of(testEvent));
        when(eventRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<AdminEventDto> result = adminService.getAllEvents(pageable);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one event");
        assertEquals("Test Event", result.content().get(0).name(), "Event name should match");
    }

    @Test
    @DisplayName("deleteEvent - given existing event - sets status to CANCELLED")
    void deleteEvent_givenExistingEvent_setsStatusToCancelled() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        adminService.deleteEvent(1L);

        assertEquals("CANCELLED", testEvent.getStatus(), "Event status should be CANCELLED");
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("deleteEvent - given nonexistent event - throws ResourceNotFoundException")
    void deleteEvent_givenNonexistentEvent_throwsResourceNotFoundException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.deleteEvent(999L),
                "Should throw ResourceNotFoundException for unknown event");
    }

    @Test
    @DisplayName("getAllUsers - given users exist - returns paged user DTOs")
    void getAllUsers_givenUsersExist_returnsPagedUserDtos() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<UserDto> result = adminService.getAllUsers(pageable);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one user");
        assertEquals("admin@example.com", result.content().get(0).email(), "Email should match");
    }

    @Test
    @DisplayName("updateUserStatus - given existing user - updates enabled flag")
    void updateUserStatus_givenExistingUser_updatesEnabledFlag() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        adminService.updateUserStatus(1L, false);

        assertEquals(false, testUser.isEnabled(), "User should be disabled");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserRoles - given valid roles - updates user roles")
    void updateUserRoles_givenValidRoles_updatesUserRoles() {
        Role adminRole = new Role("ROLE_ADMIN");
        adminRole.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        adminService.updateUserRoles(1L, Set.of("ROLE_ADMIN"));

        verify(userRepository).save(testUser);
    }
}
