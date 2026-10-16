package com.gomo.rag.dto;

import com.gomo.rag.enums.ReimbursementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayableReimbursementResponse {
    private Long reimbursementId;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String employeeContactNumber;
    private float amount;
    private String currency;
    private String reason;
    private ReimbursementStatus status;
    private String managerName;
    private String ragAnalysis;
    private String razorpayOrderId;
    private LocalDateTime createdAt;
}