package com.gomo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecordResponse {
    private Long id;
    private Long reimbursementId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private float amount;
    private String currency;
    private Long employeeId;
    private String employeeName;
    private String employeeContact;
    private String paidByEmail;
    private LocalDateTime paidAt;
}