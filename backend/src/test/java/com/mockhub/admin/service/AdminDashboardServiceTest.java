package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.admin.dto.DashboardStatsDto;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.ticket.repository.ListingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private AdminOrderService adminOrderService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("getDashboardStats - given data exists - returns aggregated stats DTO")
    void getDashboardStats_givenDataExists_returnsAggregatedStatsDto() {
        when(adminUserService.getUserCount()).thenReturn(100L);
        when(adminOrderService.getOrderCount()).thenReturn(50L);
        when(adminOrderService.getTotalRevenue()).thenReturn(new BigDecimal("10000.00"));

        Event activeEvent = new Event();
        activeEvent.setStatus("ACTIVE");
        Event completedEvent = new Event();
        completedEvent.setStatus("COMPLETED");
        when(eventRepository.findAll()).thenReturn(List.of(activeEvent, completedEvent));

        when(listingRepository.count()).thenReturn(200L);
        when(adminOrderService.getRecentOrders(anyInt())).thenReturn(List.of());

        DashboardStatsDto result = adminDashboardService.getDashboardStats();

        assertNotNull(result, "Dashboard stats should not be null");
        assertEquals(100L, result.totalUsers(), "Total users should match");
        assertEquals(50L, result.totalOrders(), "Total orders should match");
        assertEquals(new BigDecimal("10000.00"), result.totalRevenue(), "Revenue should match");
        assertEquals(1L, result.activeEvents(), "Active events should be 1");
        assertEquals(200L, result.totalListings(), "Total listings should match");
    }

    @Test
    @DisplayName("getDashboardStats - given no data - returns zero stats")
    void getDashboardStats_givenNoData_returnsZeroStats() {
        when(adminUserService.getUserCount()).thenReturn(0L);
        when(adminOrderService.getOrderCount()).thenReturn(0L);
        when(adminOrderService.getTotalRevenue()).thenReturn(BigDecimal.ZERO);
        when(eventRepository.findAll()).thenReturn(List.of());
        when(listingRepository.count()).thenReturn(0L);
        when(adminOrderService.getRecentOrders(anyInt())).thenReturn(List.of());

        DashboardStatsDto result = adminDashboardService.getDashboardStats();

        assertEquals(0L, result.totalUsers());
        assertEquals(0L, result.totalOrders());
        assertEquals(BigDecimal.ZERO, result.totalRevenue());
        assertEquals(0L, result.activeEvents());
        assertEquals(0L, result.totalListings());
    }
}
