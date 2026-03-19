package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.admin.dto.AdminEventDto;
import com.mockhub.admin.dto.DashboardStatsDto;
import com.mockhub.auth.dto.UserDto;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.CategoryDto;
import com.mockhub.event.dto.EventCreateRequest;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.TagDto;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.service.EventService;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.dto.VenueSummaryDto;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final ListingRepository listingRepository;
    private final RoleRepository roleRepository;
    private final EventService eventService;

    public AdminService(UserRepository userRepository,
                        EventRepository eventRepository,
                        OrderRepository orderRepository,
                        TicketRepository ticketRepository,
                        ListingRepository listingRepository,
                        RoleRepository roleRepository,
                        EventService eventService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.listingRepository = listingRepository;
        this.roleRepository = roleRepository;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        List<Order> confirmedOrders = orderRepository.findAll().stream()
                .filter(order -> "CONFIRMED".equals(order.getStatus()))
                .toList();
        for (Order order : confirmedOrders) {
            totalRevenue = totalRevenue.add(order.getTotal());
        }

        long activeEvents = eventRepository.findAll().stream()
                .filter(event -> "ACTIVE".equals(event.getStatus()))
                .count();

        long totalListings = listingRepository.count();

        // Get 10 most recent orders
        Page<Order> recentOrderPage = orderRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 10,
                        org.springframework.data.domain.Sort.by("createdAt").descending()));
        List<OrderSummaryDto> recentOrders = recentOrderPage.getContent().stream()
                .map(order -> new OrderSummaryDto(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        order.getTotal(),
                        order.getItems().size(),
                        order.getCreatedAt()
                ))
                .toList();

        return new DashboardStatsDto(
                totalUsers,
                totalOrders,
                totalRevenue,
                activeEvents,
                totalListings,
                recentOrders
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminEventDto> getAllEvents(Pageable pageable) {
        Page<Event> eventPage = eventRepository.findAll(pageable);

        List<AdminEventDto> content = eventPage.getContent().stream()
                .map(this::toAdminEventDto)
                .toList();

        return new PagedResponse<>(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages()
        );
    }

    @Transactional
    public EventDto createEvent(EventCreateRequest request) {
        return eventService.createEvent(request);
    }

    @Transactional
    public EventDto updateEvent(Long id, EventCreateRequest request) {
        return eventService.updateEvent(id, request);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        event.setStatus("CANCELLED");
        eventRepository.save(event);
        log.info("Admin cancelled event {} ({})", id, event.getName());
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserDto> content = userPage.getContent().stream()
                .map(this::toUserDto)
                .toList();

        return new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Transactional
    public void updateUserRoles(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
            roles.add(role);
        }

        user.setRoles(roles);
        userRepository.save(user);
        log.atInfo().setMessage("Admin updated roles for user {} to {}").addArgument(userId).addArgument(() -> roleNames.toString().replaceAll("[\\r\\n]", "")).log();
    }

    @Transactional
    public void updateUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("Admin {} user {}", enabled ? "enabled" : "disabled", userId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryDto> getAllOrders(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<OrderSummaryDto> content = orderPage.getContent().stream()
                .map(order -> new OrderSummaryDto(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        order.getTotal(),
                        order.getItems().size(),
                        order.getCreatedAt()
                ))
                .toList();

        return new PagedResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Transactional
    public int generateTicketsForEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        Venue venue = event.getVenue();
        List<Ticket> tickets = new ArrayList<>();

        for (Section section : venue.getSections()) {
            if ("GENERAL_ADMISSION".equals(section.getSectionType())) {
                // Generate GA tickets based on section capacity
                for (int i = 0; i < section.getCapacity(); i++) {
                    Ticket ticket = new Ticket();
                    ticket.setEvent(event);
                    ticket.setSection(section);
                    ticket.setTicketType("GENERAL_ADMISSION");
                    ticket.setFaceValue(event.getBasePrice());
                    ticket.setStatus("AVAILABLE");
                    ticket.setBarcode(generateBarcode());
                    tickets.add(ticket);
                }
            } else {
                // Generate seated tickets from rows and seats
                for (SeatRow seatRow : section.getSeatRows()) {
                    for (Seat seat : seatRow.getSeats()) {
                        Ticket ticket = new Ticket();
                        ticket.setEvent(event);
                        ticket.setSection(section);
                        ticket.setSeat(seat);
                        ticket.setTicketType("RESERVED");
                        ticket.setFaceValue(event.getBasePrice());
                        ticket.setStatus("AVAILABLE");
                        ticket.setBarcode(generateBarcode());
                        tickets.add(ticket);
                    }
                }
            }
        }

        List<Ticket> savedTickets = ticketRepository.saveAll(tickets);
        int ticketCount = savedTickets.size();

        // Update event ticket counts
        event.setTotalTickets(ticketCount);
        event.setAvailableTickets(ticketCount);
        eventRepository.save(event);

        log.info("Generated {} tickets for event {} ({})", ticketCount, eventId, event.getName());
        return ticketCount;
    }

    private String generateBarcode() {
        return "MH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private AdminEventDto toAdminEventDto(Event event) {
        Venue venue = event.getVenue();
        VenueSummaryDto venueSummary = new VenueSummaryDto(
                venue.getId(),
                venue.getName(),
                venue.getSlug(),
                venue.getCity(),
                venue.getState(),
                venue.getVenueType(),
                venue.getCapacity(),
                venue.getImageUrl()
        );

        CategoryDto categoryDto = new CategoryDto(
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getCategory().getSlug(),
                event.getCategory().getIcon(),
                event.getCategory().getSortOrder()
        );

        List<TagDto> tagDtos = event.getTags().stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getSlug()))
                .toList();

        int soldTickets = event.getTotalTickets() - event.getAvailableTickets();

        // Calculate revenue from confirmed orders for this event
        BigDecimal revenue = BigDecimal.ZERO;

        return new AdminEventDto(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getDescription(),
                event.getArtistName(),
                event.getEventDate(),
                event.getDoorsOpenAt(),
                event.getStatus(),
                event.getBasePrice(),
                event.getMinPrice(),
                event.getMaxPrice(),
                event.getTotalTickets(),
                event.getAvailableTickets(),
                event.isFeatured(),
                venueSummary,
                categoryDto,
                tagDtos,
                null,
                soldTickets,
                revenue
        );
    }

    private UserDto toUserDto(User user) {
        Set<String> roleNames = new HashSet<>();
        for (Role role : user.getRoles()) {
            roleNames.add(role.getName());
        }

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                roleNames,
                user.getCreatedAt()
        );
    }
}
