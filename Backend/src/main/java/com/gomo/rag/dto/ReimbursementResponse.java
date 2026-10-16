package com.gomo.rag.dto;

import com.gomo.rag.enums.ReimbursementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReimbursementResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String employeeContact;
    private Long managerId;
    private String managerName;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private String billUrl;
    private String billExtractedText;
    private String ragAnalysis;
    private ReimbursementStatus status;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}