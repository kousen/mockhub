package com.mockhub.admin.dto;

import java.util.List;

public record DemoResetResultDto(
        String userEmail,
        boolean cartCleared,
        List<String> cancelledOrders,
        List<String> revokedMandates
) {}
