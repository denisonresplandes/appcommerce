package dev.denison.appcommerce.users.presentation.dto;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth
) {
}
