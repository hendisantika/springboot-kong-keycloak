package com.ivanfranchin.bookservice.book.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(@NotBlank String isbn, @NotBlank String title) {
}
