package com.bal.bal_management_system.repository;

import com.bal.bal_management_system.model.ResetToken;
import com.bal.bal_management_system.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {

    // Delete a reset token by its token string
    void deleteByToken(String token);

    // Find a reset token by its associated user entity
    Optional<ResetToken> findByUser (UserEntity user); // Accept UserEntity directly

    // Find a reset token by the token string
    Optional<ResetToken> findByToken(String token);

    // Check if a token exists
    boolean existsByToken(String token); // Add this method
}