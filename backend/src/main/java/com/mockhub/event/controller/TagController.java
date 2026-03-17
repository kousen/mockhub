package com.mockhub.event.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.event.dto.TagDto;
import com.mockhub.event.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags", description = "Event tags for filtering")
public class TagController {

    private final EventService eventService;

    public TagController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "List all tags", description = "Return all available event tags")
    public ResponseEntity<List<TagDto>> listTags() {
        return ResponseEntity.ok(eventService.listTags());
    }
}
