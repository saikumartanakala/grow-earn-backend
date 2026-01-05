package com.growearn.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.growearn.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
