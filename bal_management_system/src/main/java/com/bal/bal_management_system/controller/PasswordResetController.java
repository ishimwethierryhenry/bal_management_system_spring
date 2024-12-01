package com.bal.bal_management_system.controller;

import com.bal.bal_management_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    @Autowired
    private UserService userService;

    // Forgot Password Form
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password"; // Forgot password form page
    }

    // Handle Forgot Password Request
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, Model model) {
        // Check if the user exists
        if (!userService.doesEmailExist(email)) {
            model.addAttribute("error", "Email address not found.");
            return "forgot-password"; // Redirect back to forgot password page
        }

        // Delete existing token if it exists
        userService.deleteExistingResetTokenByEmail(email);

        // Generate reset token and send email
        boolean emailSent = userService.sendPasswordResetEmail(email);

        if (emailSent) {
            model.addAttribute("message", "A reset link has been sent to your email.");
        } else {
            model.addAttribute("error", "Failed to send email. Please try again.");
        }

        return "forgot-password"; // Redirect back to forgot password page
    }

    // Password Reset Page
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        boolean isValidToken = userService.validateResetToken(token); // Validate the token

        if (!isValidToken) {
            model.addAttribute("error", "Invalid or expired password reset token.");
            return "forgot-password"; // Redirect back to forgot password page
        }

        model.addAttribute("token", token);
        return "reset-password"; // Password reset form page
    }

    // Handle Password Reset
    @PostMapping("/reset-password")
    public String handlePasswordReset(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmNewPassword") String confirmNewPassword,
                                      RedirectAttributes redirectAttributes) {
        System.out.println("Received token for validation: " + token); // Log the token value

        // Check if newPassword matches confirmNewPassword
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match. Please try again.");
            return "redirect:/reset-password?token=" + token; // Redirect back to the reset password page
        }

        boolean isResetSuccessful = userService.resetUserPassword(token, newPassword); // Reset the user's password

        if (isResetSuccessful) {
            redirectAttributes.addFlashAttribute("message", "Your password has been successfully reset. You can now log in.");
            return "redirect:/login"; // Redirect to login page
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to reset password. Please try again.");
            return "redirect:/reset-password?token=" + token; // Redirect back to the reset password page
        }
    }
}