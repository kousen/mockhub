package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.venue.entity.Venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminOrderService adminOrderService;

    private Event testEvent;
    private Venue testVenue;

    @BeforeEach
    void setUp() {
        testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Test Venue");
        testVenue.setSlug("test-venue");
        testVenue.setCity("New York");
        testVenue.setState("NY");
        testVenue.setVenueType("ARENA");
        testVenue.setCapacity(20000);
        testVenue.setSections(new ArrayList<>());

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
    @DisplayName("getAllOrders - given orders exist - returns paged order summaries")
    void getAllOrders_givenOrdersExist_returnsPagedOrderSummaries() {
        Listing listing = new Listing();
        listing.setEvent(testEvent);

        OrderItem item = new OrderItem();
        item.setListing(listing);
        item.setPricePaid(new BigDecimal("75.00"));

        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("MH-20260329-0001");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotal(new BigDecimal("82.50"));
        order.setCreatedAt(Instant.now());
        order.setItems(List.of(item));

        Pageable pageable = PageRequest.of(0, 20);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));

        PagedResponse<OrderSummaryDto> result = adminOrderService.getAllOrders(pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        OrderSummaryDto summary = result.content().getFirst();
        assertEquals("Test Event", summary.eventName());
        assertEquals("Test Venue", summary.venueName());
        assertNotNull(summary.eventDate());
    }

    @Test
    @DisplayName("getOrderCount - returns total order count")
    void getOrderCount_returnsTotalOrderCount() {
        when(orderRepository.count()).thenReturn(50L);

        long result = adminOrderService.getOrderCount();

        assertEquals(50L, result);
    }

    @Test
    @DisplayName("getRecentOrders - returns most recent orders as summaries")
    void getRecentOrders_returnsMostRecentOrdersAsSummaries() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("MH-20260329-0001");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotal(new BigDecimal("82.50"));
        order.setCreatedAt(Instant.now());
        order.setItems(List.of());

        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        List<OrderSummaryDto> result = adminOrderService.getRecentOrders(10);

        assertThat(result).hasSize(1);
        assertEquals("MH-20260329-0001", result.getFirst().orderNumber());
    }

    @Test
    @DisplayName("getTotalRevenue - given confirmed orders - sums totals")
    void getTotalRevenue_givenConfirmedOrders_sumsTotals() {
        Order confirmed1 = new Order();
        confirmed1.setStatus(OrderStatus.CONFIRMED);
        confirmed1.setTotal(new BigDecimal("100.00"));

        Order confirmed2 = new Order();
        confirmed2.setStatus(OrderStatus.CONFIRMED);
        confirmed2.setTotal(new BigDecimal("200.00"));

        Order pending = new Order();
        pending.setStatus(OrderStatus.PENDING);
        pending.setTotal(new BigDecimal("50.00"));

        when(orderRepository.findAll()).thenReturn(List.of(confirmed1, confirmed2, pending));

        BigDecimal result = adminOrderService.getTotalRevenue();

        assertThat(result).isEqualByComparingTo(new BigDecimal("300.00"));
    }
}
