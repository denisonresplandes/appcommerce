package dev.denison.appcommerce.users.application.exception;

import dev.denison.appcommerce.shared.application.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long id) {
        super("user not found with id %d".formatted(id));
    }

    public UserNotFoundException(String email) {
        super("user not found with email %s".formatted(email));
    }
}
