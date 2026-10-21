package com.gomo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveSubmitRequest {
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}