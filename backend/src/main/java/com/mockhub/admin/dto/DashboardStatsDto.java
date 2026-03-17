package com.mockhub.admin.dto;

import java.math.BigDecimal;
import java.util.List;

import com.mockhub.order.dto.OrderSummaryDto;

public record DashboardStatsDto(
        long totalUsers,
        long totalOrders,
        BigDecimal totalRevenue,
        long activeEvents,
        long totalListings,
        List<OrderSummaryDto> recentOrders
) {
}
