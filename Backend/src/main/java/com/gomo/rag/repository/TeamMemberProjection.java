package com.gomo.rag.repository;

import java.time.LocalDateTime;

public interface TeamMemberProjection {

    Long getId();

    String getName();

    String getEmail();

    String getContactNumber();

    String getRole();

    Long getTeamId();

    String getTeamName();

    Boolean getActive();

    LocalDateTime getCreatedAt();
}
