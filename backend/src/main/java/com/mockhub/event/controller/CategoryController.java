package com.mockhub.event.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.event.dto.CategoryDto;
import com.mockhub.event.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Event categories")
public class CategoryController {

    private final EventService eventService;

    public CategoryController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "List all categories", description = "Return all event categories sorted by display order")
    public ResponseEntity<List<CategoryDto>> listCategories() {
        return ResponseEntity.ok(eventService.listCategories());
    }
}
