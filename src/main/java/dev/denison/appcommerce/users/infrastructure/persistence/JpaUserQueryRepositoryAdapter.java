package dev.denison.appcommerce.users.infrastructure.persistence;

import dev.denison.appcommerce.users.application.port.UserQueryRepository;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.UserId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaUserQueryRepositoryAdapter implements UserQueryRepository {

    private final SpringDataUserQueryRepository springDataUserQueryRepository;

    public JpaUserQueryRepositoryAdapter(SpringDataUserQueryRepository springDataUserQueryRepository) {
        this.springDataUserQueryRepository = springDataUserQueryRepository;
    }

    @Override
    public Optional<UserView> findById(UserId id) {
        return springDataUserQueryRepository.findViewById(id.value());
    }

    @Override
    public Optional<UserView> findByEmail(Email email) {
        return springDataUserQueryRepository.findViewByEmail(email.value());
    }

    @Override
    public List<UserView> findAll() {
        return springDataUserQueryRepository.findAllViews();
    }
}
