//AdminUserController.java

package com.bal.bal_management_system.controller;

import com.bal.bal_management_system.model.AuditTrail;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.itextpdf.layout.element.Table;
import com.bal.bal_management_system.model.Role;
import com.bal.bal_management_system.model.UserEntity;
import com.bal.bal_management_system.service.UserService;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
// Add these imports
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "username") String searchCriteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        int pageSize = 3;

        Pageable pageable;
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort sort = sortDir.equalsIgnoreCase("asc") ?
                    Sort.by(sortBy).ascending() :
                    Sort.by(sortBy).descending();
            pageable = PageRequest.of(page, pageSize, sort);
        } else {
            pageable = PageRequest.of(page, pageSize);
        }

        Page<UserEntity> userPage = userService.findUsers(search, searchCriteria, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", page);
        response.put("totalPages", userPage.getTotalPages());
        response.put("totalUsers", userPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new UserEntity());
        model.addAttribute("roles", Role.values());
        return "add_user";
    }

    @PostMapping("/add")
    public String addUser (@ModelAttribute("user") UserEntity user,
                           @RequestParam("profilePicture") MultipartFile file,
                           Model model) {
        if (userService.usernameExists(user.getUsername())) {
            model.addAttribute("error", "Username already exists");
            model.addAttribute("roles", Role.values());
            return "add_user";
        }
        if (userService.emailExists(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            model.addAttribute("roles", Role.values());
            return "add_user";
        }

        if (!file.isEmpty()) {
            try {
                String uploadDir = "C:/Users/USER/Documents/Uploads/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String filePath = uploadDir + fileName;
                File destFile = new File(filePath);
                file.transferTo(destFile);
                user.setProfilePicturePath("/uploads/" + fileName);
            } catch (IOException e) {
                model.addAttribute("error", "File upload failed: " + e.getMessage());
                model.addAttribute("roles", Role.values());
                return "add_user";
            }
        }

        userService.save(user);
        // Create and save audit trail
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setAction("User  Created");
        auditTrail.setUsername(user.getUsername()); // Assuming you want to log the username of the created user
        auditTrail.setTimestamp(LocalDateTime.now());
        auditTrail.setDetails("User  " + user.getUsername() + " was created.");
        userService.saveAuditTrail(auditTrail); // Save the audit trail

        return "redirect:/admin/users";
    }

    @GetMapping("/update/{id}")
    public String showUpdateUserForm(@PathVariable("id") Long id, Model model) {
        Optional<UserEntity> userOptional = userService.getUserById(id);
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
            model.addAttribute("roles", Role.values());
            model.addAttribute("isUserProfile", false); // Flag to indicate this is an admin update
            return "update_user";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable("id") Long id,
                             @ModelAttribute("user") UserEntity user,
                             @RequestParam(value = "profilePicture", required = false) MultipartFile file,
                             Model model) {
        Optional<UserEntity> existingUserOptional = userService.getUserById(id);
        if (existingUserOptional.isEmpty()) {
            return "redirect:/admin/users";
        }

        UserEntity existingUser = existingUserOptional.get();
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setRole(user.getRole());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setDateOfBirth(user.getDateOfBirth());

        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = "C:/Users/USER/Documents/Uploads/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String filePath = uploadDir + fileName;
                File destFile = new File(filePath);
                file.transferTo(destFile);
                existingUser.setProfilePicturePath("/uploads/" + fileName);
            } catch (IOException e) {
                model.addAttribute("error", "File upload failed: " + e.getMessage());
                model.addAttribute("roles", Role.values());
                model.addAttribute("isUserProfile", false);
                return "update_user";
            }
        }

        userService.save(existingUser);

        // Create and save audit trail
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setAction("User  Updated");
        auditTrail.setUsername(existingUser .getUsername());
        auditTrail.setTimestamp(LocalDateTime.now());
        auditTrail.setDetails("User  " + existingUser .getUsername() + " was updated.");
        userService.saveAuditTrail(auditTrail); // Save the audit trail

        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser (@PathVariable("id") Long id) {
        Optional<UserEntity> userOptional = userService.getUserById(id);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();

            // Create and save audit trail
            AuditTrail auditTrail = new AuditTrail();
            auditTrail.setAction("User  Deleted");
            auditTrail.setUsername(user.getUsername());
            auditTrail.setTimestamp(LocalDateTime.now());
            auditTrail.setDetails("User  " + user.getUsername() + " was deleted.");
            userService.saveAuditTrail(auditTrail); // Save the audit trail
        }

        userService.deleteById(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadUsersCsv() {
        try {
            System.out.println("Starting CSV download");
            List<UserEntity> users = userService.getAllUsers();
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("LIST OF ALL USERS\n");
            csvContent.append("ID,Username,Email,Role\n");

            for (UserEntity user : users) {
                csvContent.append(String.format("%d,%s,%s,%s\n",
                        user.getId(), user.getUsername(), user.getEmail(), user.getRole().name()));
            }

            ByteArrayResource resource = new ByteArrayResource(csvContent.toString().getBytes());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Users_data.csv\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/pdf")
    public ResponseEntity<Resource> downloadUsersPdf() {
        try {
            System.out.println("Starting PDF download");
            List<UserEntity> users = userService.getAllUsers();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(writer));
            document.add(new Paragraph("BASKETBALL AFRICA LEAGUE").setFontSize(30).setBold());
            document.add(new Paragraph("ADMINISTRATION").setFontSize(20).setBold());
            document.add(new Paragraph("List of All Users").setFontSize(16).setBold());


            float[] columnWidths = {50, 150, 200, 100};
            Table table = new Table(columnWidths);
            table.addHeaderCell("ID");
            table.addHeaderCell("Username");
            table.addHeaderCell("Email");
            table.addHeaderCell("Role");

            for (UserEntity user : users) {
                table.addCell(String.valueOf(user.getId()));
                table.addCell(user.getUsername());
                table.addCell(user.getEmail());
                table.addCell(user.getRole().name());
            }

            document.add(table);
            document.close();

            ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Users_data.pdf\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/excel")
    public ResponseEntity<Resource> downloadUsersExcel() {
        try {
            System.out.println("Starting XLS download");
            List<UserEntity> users = userService.getAllUsers();
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Users");

            // Create header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Username");
            headerRow.createCell(2).setCellValue("Email");
            headerRow.createCell(3).setCellValue("Role");

            // Add data
            int rowNum = 1; // Start from the second row (first row is header)
            for (UserEntity user : users) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getEmail());
                row.createCell(3).setCellValue(user.getRole().name());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Users_data.xlsx\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // AdminUser Controller.java

//    @GetMapping("/admin/dashboard")  // Change the mapping
    @GetMapping("/dashboard")  // Changed to match the URL structure
    public String showAdminDashboard(HttpSession session, Model model) {
//        UserEntity loggedInAdmin = (UserEntity) session.getAttribute("loggedInUser ");
        UserEntity loggedInAdmin = (UserEntity) session.getAttribute("loggedInUser");  // Remove extra space
        System.out.println("Admin Dashboard - Username: " + loggedInAdmin.getUsername());

        if (loggedInAdmin == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equalsIgnoreCase(String.valueOf(loggedInAdmin.getRole()))) {
            return "redirect:/login";
        }

        model.addAttribute("user", loggedInAdmin);
        model.addAttribute("fullName", loggedInAdmin.getFirstName() + " " + loggedInAdmin.getLastName());
        model.addAttribute("profilePicture", loggedInAdmin.getProfilePicturePath() != null ? loggedInAdmin.getProfilePicturePath() : "/api/placeholder/80/80");

        // Fetch total users count dynamically
        long totalUsers = userService.countTotalUsers(); // Use the new method
        System.out.println("Debug - Total Users: " + totalUsers);
        System.out.println("Total Users: " + totalUsers);
        model.addAttribute("totalUsers", totalUsers);

        // Fetch audit trail data
        List<AuditTrail> auditTrails = userService.getAuditTrails(); // Assuming you have this method
        System.out.println("Debug - Audit Trails size: " + (auditTrails != null ? auditTrails.size() : "null"));
        model.addAttribute("auditTrails", auditTrails);

        return "admin_dashboard";
    }

    // Admin Login (Add this if not already present)
    @PostMapping("/admin/login")
    public String adminLogin(@ModelAttribute("user") UserEntity user,
                             HttpSession session, Model model) {
        Optional<UserEntity> authenticatedAdmin = userService.findByUsername(user.getUsername());

        if (authenticatedAdmin.isPresent() && userService.isValidUser(user.getUsername(), user.getPassword())) {
            UserEntity adminUser = authenticatedAdmin.get();

            // Set session attributes
            session.setAttribute("loggedInUser", adminUser);
            System.out.println("Logged-in Admin: " + adminUser.getUsername());
            session.setAttribute("username", adminUser.getUsername());
            session.setAttribute("role", "ADMIN");

            return "redirect:/admin_dashboard"; // Admin dashboard page
        }
        model.addAttribute("error", "Invalid credentials for admin login");
        return "admin_dashboard"; // Admin login page
    }

    // In AdminUserController class, add these methods:
    @GetMapping("/api/users/role-distribution")
    @ResponseBody
    public Map<String, Long> getUserRoleDistribution() {
        List<UserEntity> users = userService.getAllUsers();
        return users.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getRole().name(),
                        Collectors.counting()
                ));
    }

    @GetMapping("/api/users/registration-trend")
    @ResponseBody
    public List<Map<String, Object>> getUserRegistrationTrend() {
        List<UserEntity> users = userService.getAllUsers();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, Long> monthlyRegistration = users.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().format(formatter),
                        Collectors.counting()
                ));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, Long> entry : monthlyRegistration.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", entry.getKey());
            point.put("count", entry.getValue());
            trend.add(point);
        }

        return trend;
    }

}