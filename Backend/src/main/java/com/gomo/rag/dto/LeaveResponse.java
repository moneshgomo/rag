package com.gomo.rag.dto;

import com.gomo.rag.enums.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String employeeContact;
    private Long managerId;
    private String managerName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int daysCount;
    private String reason;
    private String ragDecision;
    private LeaveStatus status;
    private String managerComment;
    private LocalDateTime createdAt;
}