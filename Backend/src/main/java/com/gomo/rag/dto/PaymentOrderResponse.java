package com.gomo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderResponse {
    private String razorpayOrderId;
    private Long reimbursementId;
    private float amount;
    private String currency;
    private Long employeeId;
    private String employeeName;
    private String employeeContactNumber;
    private String employeeEmail;
    private String razorpayKeyId;
}