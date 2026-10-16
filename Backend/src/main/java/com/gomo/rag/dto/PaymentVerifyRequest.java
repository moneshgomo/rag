package com.gomo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerifyRequest {
    private Long reimbursementId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}