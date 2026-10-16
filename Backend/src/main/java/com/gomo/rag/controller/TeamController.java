package com.gomo.rag.controller;

import com.gomo.rag.dto.AddMemberRequest;
import com.gomo.rag.dto.TeamRequest;
import com.gomo.rag.dto.TeamResponse;
import com.gomo.rag.dto.UserResponse;
import com.gomo.rag.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TeamResponse> createTeam(@RequestBody TeamRequest request) {
            return ResponseEntity.ok(teamService.createTeam(request));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> listTeams() {
        return ResponseEntity.ok(teamService.listTeams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeam(id));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserResponse> addMember(@PathVariable Long id, @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(teamService.addMemberToTeam(id, request));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserResponse>> listMembers(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.listTeamMembers(id));
    }
}