package com.bookstore.dto;

import java.math.BigDecimal;
import java.util.Set;

public record BookResponse(
        Long id,
        String title,
        Long authorId,
        String authorName,
        String isbn,
        BigDecimal price,
        Set<String> tags
) {
}
