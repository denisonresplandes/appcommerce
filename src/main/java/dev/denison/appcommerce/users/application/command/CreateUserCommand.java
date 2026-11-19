package dev.denison.appcommerce.users.application.command;

import java.time.LocalDate;

public record CreateUserCommand (
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth
) {
}
