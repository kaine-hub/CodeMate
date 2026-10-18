package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Team;
import com.codemate.alpha.entity.TeamMember;
import com.codemate.alpha.entity.User;
import com.codemate.alpha.repository.ProjectRepository;
import com.codemate.alpha.repository.TeamMemberRepository;
import com.codemate.alpha.repository.TeamRepository;
import com.codemate.alpha.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private static final String OPEN = "OPEN";
    private static final String CLOSED = "CLOSED";
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TeamController(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                          UserRepository userRepository, ProjectRepository projectRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    private Map<String, Object> teamData(Team team) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("teamId", team.getTeamId());
        data.put("leaderUserId", team.getLeader().getUserId());
        data.put("leaderName", team.getLeader().getFirstName() + " " + team.getLeader().getLastName());
        data.put("teamName", team.getTeamName());
        data.put("maxMembers", team.getMaxMembers());
        data.put("currentMembers", team.getCurrentMembers());
        data.put("recruitmentStatus", team.getRecruitmentStatus());
        return data;
    }

    private Map<String, Object> memberData(TeamMember member) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("teamMemberId", member.getTeamMemberId());
        data.put("teamId", member.getTeam().getTeamId());
        data.put("teamName", member.getTeam().getTeamName());
        data.put("userId", member.getUser().getUserId());
        data.put("firstName", member.getUser().getFirstName());
        data.put("lastName", member.getUser().getLastName());
        data.put("email", member.getUser().getEmail());
        data.put("leader", member.getTeam().getLeader().getUserId().equals(member.getUser().getUserId()));
        data.put("joinedAt", member.getJoinedAt());
        return data;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTeams(@RequestParam(required = false) String status) {
        List<Team> teams = (status == null || status.isBlank()) ? teamRepository.findAll()
                : teamRepository.findByRecruitmentStatusIgnoreCase(status.trim());
        return ResponseEntity.ok(teams.stream().map(this::teamData).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        return team == null ? ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id)
                : ResponseEntity.ok(teamData(team));
    }

    @GetMapping("/leader/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTeamsByLeader(@PathVariable Long userId) {
        return ResponseEntity.ok(teamRepository.findByLeader_UserId(userId).stream().map(this::teamData).toList());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTeamsByMember(@PathVariable Long userId) {
        return ResponseEntity.ok(teamMemberRepository.findByUser_UserId(userId).stream()
                .map(tm -> teamData(tm.getTeam())).toList());
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getTeamMembers(@PathVariable Long id) {
        if (!teamRepository.existsById(id)) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        return ResponseEntity.ok(teamMemberRepository.findByTeam_TeamId(id).stream().map(this::memberData).toList());
    }

    // Example body: {"teamName":"Alpha","maxMembers":5,"leader":{"userId":1}}
    @PostMapping
    @Transactional
    public ResponseEntity<?> createTeam(@RequestBody Team team) {
        if (team.getTeamName() == null || team.getTeamName().trim().isEmpty()) return ResponseEntity.badRequest().body("Team name is required");
        if (team.getTeamName().trim().length() > 100) return ResponseEntity.badRequest().body("Team name must be at most 100 characters");
        if (team.getMaxMembers() == null || team.getMaxMembers() < 1) return ResponseEntity.badRequest().body("Max members is required and must be at least 1");
        if (team.getLeader() == null || team.getLeader().getUserId() == null) return ResponseEntity.badRequest().body("Leader (leader.userId) is required");
        Long leaderId = team.getLeader().getUserId();
        User leader = userRepository.findById(leaderId).orElse(null);
        if (leader == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found with id: " + leaderId);
        String status = team.getRecruitmentStatus() == null || team.getRecruitmentStatus().isBlank() ? OPEN : team.getRecruitmentStatus().trim().toUpperCase();
        if (!status.equals(OPEN) && !status.equals(CLOSED)) return ResponseEntity.badRequest().body("Recruitment status must be OPEN or CLOSED");
        Team newTeam = new Team();
        newTeam.setTeamName(team.getTeamName().trim());
        newTeam.setMaxMembers(team.getMaxMembers());
        newTeam.setLeader(leader);
        newTeam.setCurrentMembers((short) 1);
        newTeam.setRecruitmentStatus(team.getMaxMembers() <= 1 ? CLOSED : status);
        Team saved = teamRepository.save(newTeam);
        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(saved);
        leaderMember.setUser(leader);
        leaderMember.setJoinedAt(LocalDateTime.now());
        teamMemberRepository.save(leaderMember);
        return ResponseEntity.status(HttpStatus.CREATED).body(teamData(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateTeam(@PathVariable Long id, @RequestBody Team updated) {
        Team existing = teamRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        if (updated.getTeamName() != null) {
            String name = updated.getTeamName().trim();
            if (name.isEmpty() || name.length() > 100) return ResponseEntity.badRequest().body("Team name must be 1-100 characters");
            existing.setTeamName(name);
        }
        if (updated.getMaxMembers() != null) {
            if (updated.getMaxMembers() < existing.getCurrentMembers()) return ResponseEntity.badRequest().body("Max members cannot be less than current members (" + existing.getCurrentMembers() + ")");
            existing.setMaxMembers(updated.getMaxMembers());
        }
        if (updated.getRecruitmentStatus() != null) {
            String status = updated.getRecruitmentStatus().trim().toUpperCase();
            if (!status.equals(OPEN) && !status.equals(CLOSED)) return ResponseEntity.badRequest().body("Recruitment status must be OPEN or CLOSED");
            if (status.equals(OPEN) && existing.getCurrentMembers() >= existing.getMaxMembers()) return ResponseEntity.badRequest().body("Team is full, cannot open recruitment");
            existing.setRecruitmentStatus(status);
        }
        return ResponseEntity.ok(teamData(teamRepository.save(existing)));
    }

    @PatchMapping("/{id}/recruitment-status")
    @Transactional
    public ResponseEntity<?> updateRecruitmentStatus(@PathVariable Long id, @RequestParam String status) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        String newStatus = status.trim().toUpperCase();
        if (!newStatus.equals(OPEN) && !newStatus.equals(CLOSED)) return ResponseEntity.badRequest().body("Recruitment status must be OPEN or CLOSED");
        if (newStatus.equals(OPEN) && team.getCurrentMembers() >= team.getMaxMembers()) return ResponseEntity.badRequest().body("Team is full, cannot open recruitment");
        team.setRecruitmentStatus(newStatus);
        return ResponseEntity.ok(teamData(teamRepository.save(team)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        if (projectRepository.existsById(id)) return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot delete team: it still has projects. Delete the projects first.");
        teamMemberRepository.deleteAll(teamMemberRepository.findByTeam_TeamId(id));
        teamRepository.delete(team);
        return ResponseEntity.noContent().build();
    }
}
