package com.gomo.rag.dto;

import com.gomo.rag.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String contactNumber;
    private Role role;
    private Long teamId;
    private String teamName;
    private boolean active;
    private boolean passwordChangeRequired;
    private LocalDateTime createdAt;
}