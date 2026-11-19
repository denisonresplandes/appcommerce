package dev.denison.appcommerce.users.application.view;

import java.time.LocalDate;

public record UserView(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth
) {
}
