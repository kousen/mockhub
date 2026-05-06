package com.mockhub.commerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.commerce.dto.CommercePolicyDto;
import com.mockhub.commerce.service.CommercePolicyService;

@RestController
@RequestMapping("/api/v1/commerce/policies")
public class CommercePolicyController {

    private final CommercePolicyService commercePolicyService;

    public CommercePolicyController(CommercePolicyService commercePolicyService) {
        this.commercePolicyService = commercePolicyService;
    }

    @GetMapping("/default")
    public ResponseEntity<CommercePolicyDto> getDefaultPolicy() {
        return ResponseEntity.ok(commercePolicyService.getDefaultPolicy());
    }

    @GetMapping("/events/{eventSlug}")
    public ResponseEntity<CommercePolicyDto> getEventPolicy(@PathVariable String eventSlug) {
        return ResponseEntity.ok(commercePolicyService.getPolicyForEvent(eventSlug));
    }
}
