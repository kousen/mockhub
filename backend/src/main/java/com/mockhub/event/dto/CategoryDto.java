package com.mockhub.event.dto;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String icon,
        int sortOrder
) {
}
