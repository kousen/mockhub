package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.entity.Event;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.repository.OrderRepository;

@Service
public class AdminOrderService {

    private final OrderRepository orderRepository;

    public AdminOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryDto> getAllOrders(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<OrderSummaryDto> content = orderPage.getContent().stream()
                .map(this::toOrderSummaryDto)
                .toList();

        return new PagedResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public long getOrderCount() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDto> getRecentOrders(int limit) {
        Page<Order> recentOrderPage = orderRepository.findAll(
                PageRequest.of(0, limit, Sort.by("createdAt").descending()));
        return recentOrderPage.getContent().stream()
                .map(this::toOrderSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        List<Order> confirmedOrders = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == com.mockhub.order.entity.OrderStatus.CONFIRMED)
                .toList();
        for (Order order : confirmedOrders) {
            totalRevenue = totalRevenue.add(order.getTotal());
        }
        return totalRevenue;
    }

    private OrderSummaryDto toOrderSummaryDto(Order order) {
        String eventName = null;
        Instant eventDate = null;
        String venueName = null;
        if (!order.getItems().isEmpty()) {
            long distinctEvents = order.getItems().stream()
                    .map(item -> item.getListing().getEvent().getId())
                    .distinct()
                    .count();
            if (distinctEvents == 1) {
                Event event = order.getItems().getFirst().getListing().getEvent();
                eventName = event.getName();
                eventDate = event.getEventDate();
                if (event.getVenue() != null) {
                    venueName = event.getVenue().getName();
                }
            } else {
                eventName = "Multiple events";
            }
        }
        return new OrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getTotal(),
                order.getItems().size(),
                order.getCreatedAt(),
                eventName,
                eventDate,
                venueName,
                order.getAgentId()
        );
    }
}
