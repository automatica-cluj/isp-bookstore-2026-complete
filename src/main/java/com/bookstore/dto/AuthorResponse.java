package com.bookstore.dto;

public record AuthorResponse(
        Long id,
        String firstName,
        String lastName
) {
}
