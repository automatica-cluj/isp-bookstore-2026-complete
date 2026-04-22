package com.bookstore.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long bookId,
        String title,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
