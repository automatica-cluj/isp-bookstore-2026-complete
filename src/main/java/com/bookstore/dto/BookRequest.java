package com.bookstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record BookRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Author ID is required")
        Long authorId,

        @NotBlank(message = "ISBN is required")
        String isbn,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        BigDecimal price,

        Set<String> tags
) {
}
