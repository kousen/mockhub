package com.mockhub.auth;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.admin.controller.AdminController;
import com.mockhub.admin.service.AdminService;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.config.SecurityConfig;
import com.mockhub.order.controller.OrderController;
import com.mockhub.order.service.CalendarService;
import com.mockhub.order.service.OrderService;
import com.mockhub.ticket.controller.SellerController;
import com.mockhub.ticket.service.ListingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that authorization boundaries hold: user A cannot access user B's
 * resources, and non-admins cannot access admin endpoints.
 */
@WebMvcTest({OrderController.class, SellerController.class, AdminController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthorizationBypassTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CalendarService calendarService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ListingService listingService;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private com.mockhub.ticketmaster.service.TicketmasterSyncService ticketmasterSyncService;

    private SecurityUser userA;
    private User userAEntity;

    @BeforeEach
    void setUp() {
        userAEntity = new User();
        userAEntity.setId(1L);
        userAEntity.setEmail("userA@example.com");
        userAEntity.setPasswordHash("hashed");
        userAEntity.setFirstName("Alice");
        userAEntity.setLastName("A");
        userAEntity.setEnabled(true);
        Role userRole = new Role("ROLE_USER");
        userAEntity.setRoles(Set.of(userRole));
        userA = new SecurityUser(userAEntity);
    }

    private void authenticateAs(SecurityUser principal) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("Order ownership")
    class OrderOwnership {

        @Test
        @DisplayName("GET /orders/{orderNumber} - other user's order - returns 401")
        void getOrder_otherUsersOrder_returns401() throws Exception {
            authenticateAs(userA);
            when(userRepository.findByEmail("userA@example.com"))
                    .thenReturn(Optional.of(userAEntity));
            when(orderService.getOrder(any(User.class), eq("MH-20260301-0001")))
                    .thenThrow(new UnauthorizedException("You do not have access to this order"));

            mockMvc.perform(get("/api/v1/orders/MH-20260301-0001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /orders/{orderNumber}/calendar - other user's order - returns 401")
        void getCalendar_otherUsersOrder_returns401() throws Exception {
            authenticateAs(userA);
            when(userRepository.findByEmail("userA@example.com"))
                    .thenReturn(Optional.of(userAEntity));
            when(orderService.getOrder(any(User.class), eq("MH-20260301-0001")))
                    .thenThrow(new UnauthorizedException("You do not have access to this order"));

            mockMvc.perform(get("/api/v1/orders/MH-20260301-0001/calendar"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Listing ownership")
    class ListingOwnership {

        @Test
        @DisplayName("PUT /listings/{id}/price - other seller's listing - returns 401")
        void updatePrice_otherSellersListing_returns401() throws Exception {
            authenticateAs(userA);
            when(listingService.updateListingPrice(eq(99L), eq("userA@example.com"), any()))
                    .thenThrow(new UnauthorizedException("You do not own this listing"));

            mockMvc.perform(put("/api/v1/listings/99/price")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"price": 150.00}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /listings/{id} - other seller's listing - returns 401")
        void deactivate_otherSellersListing_returns401() throws Exception {
            authenticateAs(userA);
            doThrow(new UnauthorizedException("You do not own this listing"))
                    .when(listingService).deactivateListing(99L, "userA@example.com");

            mockMvc.perform(delete("/api/v1/listings/99"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Admin endpoint access")
    class AdminAccess {

        @Test
        @DisplayName("GET /admin/dashboard - non-admin user - returns 403")
        void dashboard_nonAdmin_returns403() throws Exception {
            authenticateAs(userA);

            mockMvc.perform(get("/api/v1/admin/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/users - non-admin user - returns 403")
        void listUsers_nonAdmin_returns403() throws Exception {
            authenticateAs(userA);

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/events - non-admin user - returns 403")
        void getEvents_nonAdmin_returns403() throws Exception {
            authenticateAs(userA);

            mockMvc.perform(get("/api/v1/admin/events"))
                    .andExpect(status().isForbidden());
        }
    }
}
