package dev.denison.appcommerce.users.infrastructure.persistence;

import dev.denison.appcommerce.users.application.view.UserView;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface SpringDataUserQueryRepository extends Repository<UserEntity, Long> {

    String SELECT_VIEW = "select new dev.denison.appcommerce.users.application.view.UserView("
            + "u.id, u.name, u.email, u.phone, u.dateOfBirth) from UserEntity u ";

    @Query(SELECT_VIEW + "where u.id = :id")
    Optional<UserView> findViewById(@Param("id") Long id);

    @Query(SELECT_VIEW + "where u.email = :email")
    Optional<UserView> findViewByEmail(@Param("email") String email);

    @Query(SELECT_VIEW + "order by u.id")
    List<UserView> findAllViews();
}
