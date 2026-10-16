package com.gomo.rag.controller;

import com.gomo.rag.dto.EmployeesList;
import com.gomo.rag.dto.ManagersList;
import com.gomo.rag.dto.RegisterRequest;
import com.gomo.rag.dto.UserResponse;
import com.gomo.rag.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/managers")
    public ResponseEntity<UserResponse> createManager(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(adminService.createManager(request));
    }



    @GetMapping("/managers")
    public ResponseEntity<List<ManagersList>> listManagers() {
        return ResponseEntity.ok(adminService.listManagers());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeesList>> listEmployees() {
        return ResponseEntity.ok(adminService.listEmployees());
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(adminService.createUser(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(adminService.listUsers(role));
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id));
    }
}