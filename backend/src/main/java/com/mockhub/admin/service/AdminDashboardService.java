package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.admin.dto.DashboardStatsDto;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.ticket.repository.ListingRepository;

@Service
public class AdminDashboardService {

    private final AdminUserService adminUserService;
    private final AdminOrderService adminOrderService;
    private final EventRepository eventRepository;
    private final ListingRepository listingRepository;

    public AdminDashboardService(AdminUserService adminUserService,
                                 AdminOrderService adminOrderService,
                                 EventRepository eventRepository,
                                 ListingRepository listingRepository) {
        this.adminUserService = adminUserService;
        this.adminOrderService = adminOrderService;
        this.eventRepository = eventRepository;
        this.listingRepository = listingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalUsers = adminUserService.getUserCount();
        long totalOrders = adminOrderService.getOrderCount();
        BigDecimal totalRevenue = adminOrderService.getTotalRevenue();

        long activeEvents = eventRepository.findAll().stream()
                .filter(event -> "ACTIVE".equals(event.getStatus()))
                .count();

        long totalListings = listingRepository.count();

        List<OrderSummaryDto> recentOrders = adminOrderService.getRecentOrders(10);

        return new DashboardStatsDto(
                totalUsers,
                totalOrders,
                totalRevenue,
                activeEvents,
                totalListings,
                recentOrders
        );
    }
}
