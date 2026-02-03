package com.growearn.repository;

import com.growearn.entity.Role;
import com.growearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndRole(String email, Role role);

    List<User> findByRole(Role role);

    Optional<User> findByDeviceFingerprint(String deviceFingerprint);

    Page<User> findByRole(Role role, Pageable pageable);
}
