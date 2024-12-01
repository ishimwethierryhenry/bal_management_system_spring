//UserProfileController.java

package com.bal.bal_management_system.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.bal.bal_management_system.model.UserEntity;
import com.bal.bal_management_system.service.UserService;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/user/profile")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String viewProfile(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<UserEntity> userOptional = userService.findByUsername(username);
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
            model.addAttribute("isUserProfile", true); // Flag to indicate this is a user profile update
            return "update_user";
        }
        return "redirect:/login";
    }

    @PostMapping("/update")
    public String updateProfile(
            @ModelAttribute("user") UserEntity user,
            @RequestParam(value = "profilePicture", required = false) MultipartFile file,
            HttpSession session,
            Model model) {

        String username = (String) session.getAttribute("username");

        if (username == null) {
            return "redirect:/login";
        }

        Optional<UserEntity> existingUserOptional = userService.findByUsername(username);
        if (existingUserOptional.isEmpty()) {
            return "redirect:/login";
        }

        UserEntity existingUser = existingUserOptional.get();

        // Update allowed fields
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setDateOfBirth(user.getDateOfBirth());

        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = "C:/Users/USER/Documents/Uploads/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String filePath = uploadDir + fileName;
                file.transferTo(new File(filePath));
                existingUser.setProfilePicturePath("/uploads/" + fileName);
            } catch (IOException e) {
                model.addAttribute("error", "File upload failed: " + e.getMessage());
                return "update_user";
            }
        }

        // Save updated user and set in session
        userService.save(existingUser);
        session.setAttribute("loggedInUser", existingUser);
        session.setAttribute("username", existingUser.getUsername());
        session.setAttribute("profilePicture", existingUser.getProfilePicturePath());

        return "redirect:/user_dashboard";
    }
}