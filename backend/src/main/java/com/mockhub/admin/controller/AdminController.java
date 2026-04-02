package com.mockhub.admin.controller;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.admin.dto.AdminEventDto;
import com.mockhub.admin.dto.DashboardStatsDto;
import com.mockhub.admin.service.AdminService;
import com.mockhub.auth.dto.UserDto;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.dto.EventCreateRequest;
import com.mockhub.event.dto.EventDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.ticketmaster.service.TicketmasterSyncService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Administrative operations (ROLE_ADMIN required)")
public class AdminController {

    private final AdminService adminService;
    private final Optional<TicketmasterSyncService> ticketmasterSyncService;

    public AdminController(AdminService adminService,
                           Optional<TicketmasterSyncService> ticketmasterSyncService) {
        this.adminService = adminService;
        this.ticketmasterSyncService = ticketmasterSyncService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard stats", description = "Return aggregate statistics for the admin dashboard")
    @ApiResponse(responseCode = "200", description = "Dashboard stats returned")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/events")
    @Operation(summary = "List all events (admin)", description = "Return all events with admin-level details")
    @ApiResponse(responseCode = "200", description = "Events returned")
    public ResponseEntity<PagedResponse<AdminEventDto>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        return ResponseEntity.ok(adminService.getAllEvents(pageable));
    }

    @PostMapping("/events")
    @Operation(summary = "Create event (admin)", description = "Create a new event")
    @ApiResponse(responseCode = "201", description = "Event created")
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventDto event = adminService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Update event (admin)", description = "Update an existing event")
    @ApiResponse(responseCode = "200", description = "Event updated")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.ok(adminService.updateEvent(id, request));
    }

    @DeleteMapping("/events/{id}")
    @Operation(summary = "Delete event (admin)", description = "Delete an event by ID")
    @ApiResponse(responseCode = "204", description = "Event deleted")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        adminService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (admin)", description = "Return all users with pagination")
    @ApiResponse(responseCode = "200", description = "Users returned")
    public ResponseEntity<PagedResponse<UserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @PutMapping("/users/{id}/roles")
    @Operation(summary = "Update user roles (admin)", description = "Set roles for a user")
    @ApiResponse(responseCode = "204", description = "Roles updated")
    public ResponseEntity<Void> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Set<String> roles) {
        adminService.updateUserRoles(id, roles);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Enable/disable user (admin)", description = "Enable or disable a user account")
    @ApiResponse(responseCode = "204", description = "User status updated")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().build();
        }
        adminService.updateUserStatus(id, enabled);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    @Operation(summary = "List all orders (admin)", description = "Return all orders with pagination")
    @ApiResponse(responseCode = "200", description = "Orders returned")
    public ResponseEntity<PagedResponse<OrderSummaryDto>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getAllOrders(pageable));
    }

    @PostMapping("/events/{id}/generate-tickets")
    @Operation(summary = "Generate tickets (admin)", description = "Generate tickets for all seats at the event's venue")
    @ApiResponse(responseCode = "200", description = "Tickets generated")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<Map<String, Integer>> generateTickets(@PathVariable Long id) {
        int ticketCount = adminService.generateTicketsForEvent(id);
        return ResponseEntity.ok(Map.of("ticketsGenerated", ticketCount));
    }

    @PostMapping("/ticketmaster/sync")
    @Operation(summary = "Trigger Ticketmaster sync (admin)",
            description = "Manually trigger a Ticketmaster event sync. Requires the ticketmaster profile to be active.")
    @ApiResponse(responseCode = "202", description = "Sync triggered")
    @ApiResponse(responseCode = "503", description = "Ticketmaster integration not active")
    public ResponseEntity<Map<String, String>> triggerTicketmasterSync() {
        if (ticketmasterSyncService.isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Ticketmaster integration is not active. Enable the 'ticketmaster' profile.");
            problem.setTitle("Ticketmaster Not Available");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("detail", problem.getDetail(), "title", problem.getTitle()));
        }
        ticketmasterSyncService.get().syncEvents();
        return ResponseEntity.accepted().body(Map.of("status", "Sync triggered successfully"));
    }

    @PostMapping("/ticketmaster/backfill-spotify")
    @Operation(summary = "Backfill Spotify artist IDs (admin)",
            description = "Fetch individual event details from Ticketmaster to fill in missing Spotify artist IDs.")
    @ApiResponse(responseCode = "200", description = "Backfill completed")
    @ApiResponse(responseCode = "503", description = "Ticketmaster integration not active")
    public ResponseEntity<Map<String, Object>> backfillSpotifyIds() {
        if (ticketmasterSyncService.isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Ticketmaster integration is not active. Enable the 'ticketmaster' profile.");
            problem.setTitle("Ticketmaster Not Available");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("detail", problem.getDetail(), "title", problem.getTitle()));
        }
        int updated = ticketmasterSyncService.get().backfillSpotifyIds();
        return ResponseEntity.ok(Map.of("status", "Backfill completed", "eventsUpdated", updated));
    }

    @PostMapping("/ticketmaster/activate")
    @Operation(summary = "Activate Ticketmaster events (admin)",
            description = "Deactivate seed events, feature Ticketmaster events, and complete past events.")
    @ApiResponse(responseCode = "200", description = "Activation complete")
    public ResponseEntity<Map<String, Integer>> activateTicketmasterEvents() {
        return ResponseEntity.ok(adminService.activateTicketmasterEvents());
    }
}
