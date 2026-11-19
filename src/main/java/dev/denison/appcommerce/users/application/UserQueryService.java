package dev.denison.appcommerce.users.application;

import dev.denison.appcommerce.users.application.exception.UserNotFoundException;
import dev.denison.appcommerce.users.application.port.UserQueryRepository;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserQueryRepository repository;

    public UserQueryService(UserQueryRepository repository) {
        this.repository = repository;
    }

    public UserView findById(Long id) {
        return repository.findById(new UserId(id))
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public UserView findByEmail(String email) {
        Email normalizedEmail = new Email(email);
        return repository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail.value()));
    }

    public List<UserView> findAll() {
        return repository.findAll();
    }
}
