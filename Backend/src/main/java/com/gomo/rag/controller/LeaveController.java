package com.gomo.rag.controller;

import com.gomo.rag.dto.LeaveActionRequest;
import com.gomo.rag.dto.LeaveBalanceResponse;
import com.gomo.rag.dto.LeaveResponse;
import com.gomo.rag.dto.LeaveSubmitRequest;
import com.gomo.rag.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> submitLeave(@RequestBody LeaveSubmitRequest request) {
        return ResponseEntity.ok(leaveService.submitLeave(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Object>> getMyLeaves() {
        return ResponseEntity.ok(leaveService.getMyLeaveRequest());
    }

    @GetMapping("/balance")
    public ResponseEntity<LeaveBalanceResponse> getMyBalance() {
        return ResponseEntity.ok(leaveService.getMyBalance());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<List<LeaveResponse>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeavesForManager());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<LeaveResponse> approveLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest request) {

        return ResponseEntity.ok(
                leaveService.approveLeave(id, request)
        );
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<LeaveResponse> rejectLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest request) {

        return ResponseEntity.ok(
                leaveService.rejectLeave(id, request)
        );
    }}