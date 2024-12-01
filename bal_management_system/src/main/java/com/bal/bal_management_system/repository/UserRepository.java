package com.bal.bal_management_system.repository;

import com.bal.bal_management_system.model.Role;
import com.bal.bal_management_system.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Find a user by username
    Optional<UserEntity> findByUsername(String username);

    // Find a user by email
    Optional<UserEntity> findByEmail(String email);

    // Check if a username exists
    boolean existsByUsername(String username);

    // Check if an email exists
    boolean existsByEmail(String email);

    // Find users by username containing a search keyword
    Page<UserEntity> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<UserEntity> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<UserEntity> findByRole(Role role, Pageable pageable);



    // Find all users sorted by role
// Find all users sorted by role in ascending order
    Page<UserEntity> findAllByOrderByRoleAsc(Pageable pageable);

    // Find all users sorted by role in descending order
    Page<UserEntity> findAllByOrderByRoleDesc(Pageable pageable);
    Page<UserEntity> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrRole(
            String username, String email, Role role, Pageable pageable);


}
