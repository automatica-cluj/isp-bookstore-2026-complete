package com.bookstore.dto;

public record CartItemRequest(
        Long bookId,
        int quantity
) {
}
