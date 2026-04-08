package com.mockhub.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.admin.dto.DemoResetResultDto;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.order.service.OrderService;

@Service
public class DemoResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final MandateService mandateService;
    private final MandateRepository mandateRepository;

    public DemoResetService(UserRepository userRepository,
                            CartService cartService,
                            OrderService orderService,
                            OrderRepository orderRepository,
                            MandateService mandateService,
                            MandateRepository mandateRepository) {
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.mandateService = mandateService;
        this.mandateRepository = mandateRepository;
    }

    @Transactional
    public DemoResetResultDto resetUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        // 1. Clear cart
        cartService.clearCart(user);
        boolean cartCleared = true;

        // 2. Cancel/fail active orders (catch per-order so one failure doesn't abort the reset)
        List<String> cancelledOrders = new ArrayList<>();
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 1000));
        for (Order order : orders) {
            try {
                if (order.getStatus() == OrderStatus.PENDING) {
                    orderService.failOrder(order.getOrderNumber());
                    cancelledOrders.add(order.getOrderNumber());
                } else if (order.getStatus() == OrderStatus.CONFIRMED) {
                    orderService.cancelOrder(order.getOrderNumber());
                    cancelledOrders.add(order.getOrderNumber());
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to reset order {}: {}", order.getOrderNumber(), ex.getMessage());
            }
        }

        // 3. Revoke all active mandates
        List<String> revokedMandates = new ArrayList<>();
        List<Mandate> mandates = mandateRepository.findByUserEmail(userEmail);
        for (Mandate mandate : mandates) {
            if ("ACTIVE".equals(mandate.getStatus())) {
                mandateService.revokeMandate(mandate.getMandateId());
                revokedMandates.add(mandate.getMandateId());
            }
        }

        log.info("Demo reset for user '{}': cart cleared, {} orders cancelled/failed, {} mandates revoked",
                userEmail, cancelledOrders.size(), revokedMandates.size());

        return new DemoResetResultDto(
                userEmail,
                cartCleared,
                cancelledOrders,
                revokedMandates
        );
    }
}
