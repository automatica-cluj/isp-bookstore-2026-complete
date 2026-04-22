package com.bookstore.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        List<OrderItemResponse> items,
        LocalDateTime placedAt,
        String status
) {
}
