package dev.denison.appcommerce.users.presentation.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateUserRequest(

        @NotBlank(message = "{user.name.required}")
        @Size(min = 2, max = 100, message = "{user.name.size}")
        String name,

        @NotBlank(message = "{user.email.required}")
        @Email(message = "{user.email.invalid}")
        @Size(max = 100, message = "{user.email.size}")
        String email,

        @NotBlank(message = "{user.phone.required}")
        @Size(min = 10, max = 11, message = "{user.phone.size}")
        @Pattern(regexp = "\\d*", message = "{user.phone.digits}")
        String phone,

        @NotNull(message = "{user.dateOfBirth.required}")
        @PastOrPresent(message = "{user.dateOfBirth.future}")
        LocalDate dateOfBirth
) {
}
