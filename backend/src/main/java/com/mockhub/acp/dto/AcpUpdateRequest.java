package com.mockhub.acp.dto;

import java.util.List;

import jakarta.validation.Valid;

public record AcpUpdateRequest(
        @Valid
        List<AcpLineItem> addItems,
        List<Long> removeListingIds
) {
}
