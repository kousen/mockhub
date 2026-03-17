package com.mockhub.admin.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/events")
    public ResponseEntity<PagedResponse<AdminEventDto>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        return ResponseEntity.ok(adminService.getAllEvents(pageable));
    }

    @PostMapping("/events")
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventDto event = adminService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.ok(adminService.updateEvent(id, request));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        adminService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<Void> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Set<String> roles) {
        adminService.updateUserRoles(id, roles);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/status")
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
    public ResponseEntity<PagedResponse<OrderSummaryDto>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getAllOrders(pageable));
    }

    @PostMapping("/events/{id}/generate-tickets")
    public ResponseEntity<Map<String, Integer>> generateTickets(@PathVariable Long id) {
        int ticketCount = adminService.generateTicketsForEvent(id);
        return ResponseEntity.ok(Map.of("ticketsGenerated", ticketCount));
    }
}
