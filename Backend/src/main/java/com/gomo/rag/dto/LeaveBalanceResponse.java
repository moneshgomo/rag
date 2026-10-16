package com.gomo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceResponse {
    private Long employeeId;
    private String employeeName;
    private int year;
    private int totalDays;
    private int usedDays;
    private int remainingDays;
}