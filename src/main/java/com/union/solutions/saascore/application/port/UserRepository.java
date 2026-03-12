package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  User save(User user);

  Optional<User> findById(UUID id);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
