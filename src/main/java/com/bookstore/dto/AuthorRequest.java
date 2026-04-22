package com.bookstore.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName
) {
}
