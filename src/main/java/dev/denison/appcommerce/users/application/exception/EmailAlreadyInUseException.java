package dev.denison.appcommerce.users.application.exception;

import dev.denison.appcommerce.shared.application.ConflictException;

public class EmailAlreadyInUseException extends ConflictException {

    public EmailAlreadyInUseException(String email) {
        super("user email address %s is already in use".formatted(email));
    }
}
