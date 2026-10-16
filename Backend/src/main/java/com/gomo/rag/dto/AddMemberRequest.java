package com.gomo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMemberRequest {
    private Long userId; // Existing user ID (optional)
    private String name; // Or new user registration
    private String email;
    private String password;
    private String contactNumber;
}