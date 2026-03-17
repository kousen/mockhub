package com.mockhub.event.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.event.dto.CategoryDto;
import com.mockhub.event.service.EventService;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final EventService eventService;

    public CategoryController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> listCategories() {
        return ResponseEntity.ok(eventService.listCategories());
    }
}
