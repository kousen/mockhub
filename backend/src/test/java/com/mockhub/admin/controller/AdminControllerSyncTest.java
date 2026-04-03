package com.mockhub.admin.controller;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminControllerSyncTest {

    @Test
    void triggerTicketmasterSync_givenNoSyncService_returns503() {
        AdminController controller = new AdminController(
                mock(com.mockhub.admin.service.AdminDashboardService.class),
                mock(com.mockhub.admin.service.AdminEventService.class),
                mock(com.mockhub.admin.service.AdminUserService.class),
                mock(com.mockhub.admin.service.AdminOrderService.class),
                Optional.empty());

        ResponseEntity<Map<String, String>> response = controller.triggerTicketmasterSync();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsKey("detail");
    }

    @Test
    void triggerTicketmasterSync_givenSyncServicePresent_callsSyncAndReturns200() {
        com.mockhub.ticketmaster.service.TicketmasterSyncService syncService =
                mock(com.mockhub.ticketmaster.service.TicketmasterSyncService.class);
        AdminController controller = new AdminController(
                mock(com.mockhub.admin.service.AdminDashboardService.class),
                mock(com.mockhub.admin.service.AdminEventService.class),
                mock(com.mockhub.admin.service.AdminUserService.class),
                mock(com.mockhub.admin.service.AdminOrderService.class),
                Optional.of(syncService));

        ResponseEntity<Map<String, String>> response = controller.triggerTicketmasterSync();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "Sync triggered successfully");
        verify(syncService).syncEvents();
    }
}
