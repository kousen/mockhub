package com.mockhub.event.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.event.dto.TagDto;
import com.mockhub.event.service.EventService;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final EventService eventService;

    public TagController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<TagDto>> listTags() {
        return ResponseEntity.ok(eventService.listTags());
    }
}
