package dev.denison.appcommerce.users.presentation;

import dev.denison.appcommerce.users.application.UserCommandService;
import dev.denison.appcommerce.users.application.UserQueryService;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.presentation.dto.CreateUserRequest;
import dev.denison.appcommerce.users.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserQueryService queryService;
    private final UserCommandService commandService;

    public UserController(UserQueryService queryService, UserCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid CreateUserRequest request,
                                               UriComponentsBuilder uriBuilder) {
        UserView created = commandService.create(UserWebMapper.toCommand(request));
        UserResponse response = UserWebMapper.toResponse(created);
        URI location = uriBuilder.path("/users/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable
                                 @Positive(message = "{user.id.positive}")
                                 Long id) {
        return UserWebMapper.toResponse(queryService.findById(id));
    }

    @GetMapping(params = "email")
    public UserResponse findByEmail(@RequestParam
                                    @NotBlank(message = "{user.email.required}")
                                    @Email(message = "{user.email.invalid}")
                                    @Size(max = 100, message = "{user.email.size}")
                                    String email) {
        return UserWebMapper.toResponse(queryService.findByEmail(email));
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return queryService.findAll()
                .stream()
                .map(UserWebMapper::toResponse)
                .toList();
    }
}
