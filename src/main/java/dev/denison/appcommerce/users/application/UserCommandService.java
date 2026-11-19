package dev.denison.appcommerce.users.application;

import dev.denison.appcommerce.users.application.command.CreateUserCommand;
import dev.denison.appcommerce.users.application.exception.EmailAlreadyInUseException;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.domain.User;
import dev.denison.appcommerce.users.domain.UserRepository;
import dev.denison.appcommerce.users.domain.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserCommandService {

    private final UserRepository repository;

    public UserCommandService(UserRepository repository) {
        this.repository = repository;
    }

    public UserView create(CreateUserCommand command) {
        Name name = new Name(command.name());
        Email email = new Email(command.email());
        Phone phone = new Phone(command.phone());
        DateOfBirth dateOfBirth = new DateOfBirth(command.dateOfBirth());

        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email.value());
        }

        User savedUser = repository.save(User.create(name, email, phone, dateOfBirth));
        return toView(savedUser);
    }

    private UserView toView(User user) {
        return new UserView(
                user.getId().value(),
                user.getName().value(),
                user.getEmail().value(),
                user.getPhone().value(),
                user.getDateOfBirth().value()
        );
    }
}
