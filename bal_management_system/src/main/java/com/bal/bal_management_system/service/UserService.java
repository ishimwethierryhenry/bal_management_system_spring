//UserService.java

package com.bal.bal_management_system.service;

import com.bal.bal_management_system.model.AuditTrail;
import com.bal.bal_management_system.model.ResetToken;
import com.bal.bal_management_system.model.Role;
import com.bal.bal_management_system.model.UserEntity;
import com.bal.bal_management_system.repository.AuditTrailRepository;
import com.bal.bal_management_system.repository.ResetTokenRepository;
import com.bal.bal_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResetTokenRepository resetTokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AuditTrailRepository auditTrailRepository; // Add this line

    // Create or Update User
    public UserEntity save(UserEntity userEntity) {
        return userRepository.save(userEntity);
    }

    // Retrieve User by Username
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Check if email exists
    public boolean doesEmailExist(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    // Retrieve User Role by Username
    public Role getRoleByUsername(String username) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        return user.map(UserEntity::getRole).orElse(null); // Return the role if user is found, else null
    }

    // Retrieve User by ID
    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Retrieve All Users
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    // Delete User by ID
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    // Retrieve User by Email
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Check if Username Exists
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // Check if Email Exists
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // Validate User by Username and Password
    public boolean isValidUser (String username, String password) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        return user.isPresent() && user.get().getPassword().equals(password);
    }

    // New method to find users with pagination, search, and sorting by role
    public Page<UserEntity> findUsers(String searchKeyword, String searchCriteria, Pageable pageable) {
        if (searchKeyword == null || searchKeyword.isEmpty()) {
            return userRepository.findAll(pageable);
        }

        switch (searchCriteria) {
            case "username":
                return userRepository.findByUsernameContainingIgnoreCase(searchKeyword, pageable);
            case "email":
                return userRepository.findByEmailContainingIgnoreCase(searchKeyword, pageable);
            case "role":
                try {
                    Role role = Role.valueOf(searchKeyword.toUpperCase());
                    return userRepository.findByRole(role, pageable);
                } catch (IllegalArgumentException e) {
                    // If role parsing fails, return an empty page
                    return Page.empty(pageable);
                }
            default:
                // If no specific criteria, do a broad search
                Role role = null;
                try {
                    role = Role.valueOf(searchKeyword.toUpperCase());
                    return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrRole(
                            searchKeyword, searchKeyword, Role.valueOf(String.valueOf(role)), pageable);
                } catch (IllegalArgumentException e) {
                    return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrRole(
                            searchKeyword, searchKeyword, Role.valueOf(String.valueOf(role)), pageable);
                }
        }
    }


    // Send email utility
    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("ishimweth@gmail.com");  // Make sure this is set
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // Update User
    public void update(UserEntity user) {
        // Optionally handle specific update logic here
        userRepository.save(user);
    }

    // Send Password Reset Email
    @Transactional // Ensure transactional context
    public boolean sendPasswordResetEmail(String email) {
        // Retrieve user by email
        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (!user.isPresent()) {
            return false; // User not found
        }

        // Delete any existing reset token for this user before generating a new one
        deleteExistingResetTokenByEmail(email);

        // Generate a new token and save it
        String token = UUID.randomUUID().toString();
        saveResetTokenForUser(user.get(), token); // Pass the UserEntity instead of Optional

        // Generate reset URL
        String resetUrl = "http://localhost:8080/reset-password?token=" + token;

        // Retrieve user's first and last name
        String firstName = user.get().getFirstName(); // Assumes UserEntity has a getFirstName() method
        String lastName = user.get().getLastName(); // Assumes UserEntity has a getLastName() method
        String fullName = firstName + " " + lastName;

        // Prepare the email content
        String subject = "BAL Password Reset Link - Get Back in the Game!";
        String message = String.format(
                "Hey %s,\n\n" +
                        "No worries if you forgot your Basketball Africa League (BAL) password! We all have those days.\n\n" +
                        "Here's your chance to get back in the action and witness the incredible talent of Africa's best. Click the link below to reset your password and continue following the season under the motto: \"Where Dreams Are Made.\"\n\n" +
                        "Reset Password Link: %s\n\n" +
                        "Once you click the link, you'll be directed to a page where you can create a strong new password.\n\n" +
                        "Stay Updated with BAL!\n\n" +
                        "While you're at it, be sure to check out the latest news, highlights, and upcoming games on the BAL website: [BAL Website Link (bal.nba.com)]\n\n" +
                        "We can't wait to see you back on the court (virtually, of course)!\n\n" +
                        "The BAL Team",
                fullName, resetUrl
        );

        // Send the email
        sendEmail(email, subject, message);

        return true;
    }


    @Transactional // Ensure transactional context
    public void saveResetTokenForUser (UserEntity user, String token) {
        ResetToken resetToken = new ResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15)); // Set token expiry to 15 minutes

        resetTokenRepository.save(resetToken);
    }

    @Transactional // Ensure transactional context
    public void deleteExistingResetTokenByEmail(String email) {
        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            resetTokenRepository.findByUser(user.get()).ifPresent(resetToken -> {
                resetTokenRepository.delete(resetToken);
                System.out.println("Deleted existing token: " + resetToken.getToken());
            });
        }
    }

    // Validate Reset Token
    public boolean validateResetToken(String token) {
        return resetTokenRepository.existsByToken(token);
    }

    // Reset User Password
    public boolean resetUserPassword(String token, String newPassword) {
        Optional<ResetToken> resetToken = resetTokenRepository.findByToken(token);
        if (resetToken.isPresent()) {
            UserEntity user = resetToken.get().getUser();
            user.setPassword(newPassword); // Make sure to hash the password before saving
            userRepository.save(user);
            resetTokenRepository.delete(resetToken.get()); // Optionally delete the token after use
            return true;
        }
        return false;
    }
    public List<AuditTrail> getAuditTrails() {
        // Assuming you have an AuditTrailRepository to fetch audit trails
        return auditTrailRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    // Add this method to the UserService class
    public long countTotalUsers() {
        return userRepository.count(); // This will return the total number of users
    }
    public void saveAuditTrail(AuditTrail auditTrail) {
        auditTrailRepository.save(auditTrail);
    }

}