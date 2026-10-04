package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Team;
import com.codemate.alpha.entity.TeamMember;
import com.codemate.alpha.entity.User;
import com.codemate.alpha.repository.ProjectRepository;
import com.codemate.alpha.repository.TeamMemberRepository;
import com.codemate.alpha.repository.TeamRepository;
import com.codemate.alpha.repository.UserRepository;
import com.codemate.alpha.response.TeamMemberResponse;
import com.codemate.alpha.response.TeamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private static final String OPEN = "OPEN";
    private static final String CLOSED = "CLOSED";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TeamController(TeamRepository teamRepository,
                          TeamMemberRepository teamMemberRepository,
                          UserRepository userRepository,
                          ProjectRepository projectRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    // GET ALL TEAMS (optional filter: ?status=OPEN or ?status=CLOSED)
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams(
            @RequestParam(required = false) String status) {

        List<Team> teams = (status == null || status.isBlank())
                ? teamRepository.findAll()
                : teamRepository.findByRecruitmentStatusIgnoreCase(status.trim());

        return ResponseEntity.ok(teams.stream().map(TeamResponse::from).toList());
    }

    // GET TEAM BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable Long id) {
        return teamRepository.findById(id)
                .<ResponseEntity<?>>map(team -> ResponseEntity.ok(TeamResponse.from(team)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id));
    }

    // GET TEAMS LED BY A USER
    @GetMapping("/leader/{userId}")
    public ResponseEntity<List<TeamResponse>> getTeamsByLeader(@PathVariable Long userId) {
        return ResponseEntity.ok(
                teamRepository.findByLeader_UserId(userId)
                        .stream().map(TeamResponse::from).toList());
    }

    // GET ALL TEAMS A USER BELONGS TO
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TeamResponse>> getTeamsByMember(@PathVariable Long userId) {
        return ResponseEntity.ok(
                teamMemberRepository.findByUser_UserId(userId)
                        .stream()
                        .map(tm -> TeamResponse.from(tm.getTeam()))
                        .toList());
    }

    // GET MEMBERS OF A TEAM
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getTeamMembers(@PathVariable Long id) {
        if (!teamRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        }
        return ResponseEntity.ok(
                teamMemberRepository.findByTeam_TeamId(id)
                        .stream().map(TeamMemberResponse::from).toList());
    }

    // CREATE TEAM
    // Body: { "teamName": "Alpha", "maxMembers": 5, "leader": { "userId": 1 } }
    // The leader is automatically added as the first team member.
    @PostMapping
    @Transactional
    public ResponseEntity<?> createTeam(@RequestBody Team team) {

        if (team.getTeamName() == null || team.getTeamName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Team name is required");
        }
        if (team.getTeamName().trim().length() > 100) {
            return ResponseEntity.badRequest().body("Team name must be at most 100 characters");
        }
        if (team.getMaxMembers() == null || team.getMaxMembers() < 1) {
            return ResponseEntity.badRequest().body("Max members is required and must be at least 1");
        }
        if (team.getLeader() == null || team.getLeader().getUserId() == null) {
            return ResponseEntity.badRequest().body("Leader (leader.userId) is required");
        }

        User leader = userRepository.findById(team.getLeader().getUserId()).orElse(null);
        if (leader == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Leader not found with id: " + team.getLeader().getUserId());
        }

        String status = OPEN;
        if (team.getRecruitmentStatus() != null && !team.getRecruitmentStatus().isBlank()) {
            status = team.getRecruitmentStatus().trim().toUpperCase();
            if (!status.equals(OPEN) && !status.equals(CLOSED)) {
                return ResponseEntity.badRequest().body("Recruitment status must be OPEN or CLOSED");
            }
        }

        Team newTeam = new Team();
        newTeam.setTeamName(team.getTeamName().trim());
        newTeam.setMaxMembers(team.getMaxMembers());
        newTeam.setLeader(leader);
        newTeam.setCurrentMembers((short) 1); // the leader
        // A team that is already full at creation (maxMembers = 1) can't recruit
        newTeam.setRecruitmentStatus(team.getMaxMembers() <= 1 ? CLOSED : status);

        Team saved = teamRepository.save(newTeam);

        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(saved);
        leaderMember.setUser(leader);
        leaderMember.setJoinedAt(LocalDateTime.now());
        teamMemberRepository.save(leaderMember);

        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(saved));
    }

    // UPDATE TEAM (name, maxMembers, recruitmentStatus). Leader and currentMembers can't be changed here.
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateTeam(@PathVariable Long id, @RequestBody Team updated) {

        Team existing = teamRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        }

        if (updated.getTeamName() != null) {
            if (updated.getTeamName().trim().isEmpty() || updated.getTeamName().trim().length() > 100) {
                return ResponseEntity.badRequest().body("Team name must be 1-100 characters");
            }
            existing.setTeamName(updated.getTeamName().trim());
        }

        if (updated.getMaxMembers() != null) {
            if (updated.getMaxMembers() < existing.getCurrentMembers()) {
                return ResponseEntity.badRequest()
                        .body("Max members cannot be less than current members (" + existing.getCurrentMembers() + ")");
            }
            existing.setMaxMembers(updated.getMaxMembers());
        }

        if (updated.getRecruitmentStatus() != null) {
            String status = updated.getRecruitmentStatus().trim().toUpperCase();
            if (!status.equals(OPEN) && !status.equals(CLOSED)) {
                return ResponseEntity.badRequest().body("Recruitment status must be OPEN or CLOSED");
            }
            if (status.equals(OPEN) && existing.getCurrentMembers() >= existing.getMaxMembers()) {
                return ResponseEntity.badRequest().body("Team is full, cannot open recruitment");
            }
            existing.setRecruitmentStatus(status);
        }

        return ResponseEntity.ok(TeamResponse.from(teamRepository.save(existing)));
    }

    // OPEN / CLOSE RECRUITMENT: PATCH /api/teams/{id}/recruitment-status?status=CLOSED
    @PatchMapping("/{id}/recruitment-status")
    @Transactional
    public ResponseEntity<?> updateRecruitmentStatus(@PathVariable Long id,
                                                     @RequestParam String status) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        }

        String newStatus = status.trim().toUpperCase();
        if (!newStatus.equals(OPEN) && !newStatus.equals(CLOSED)) {
            return ResponseEntity.badRequest().body("Recruitment status must be OPEN or CLOSED");
        }
        if (newStatus.equals(OPEN) && team.getCurrentMembers() >= team.getMaxMembers()) {
            return ResponseEntity.badRequest().body("Team is full, cannot open recruitment");
        }

        team.setRecruitmentStatus(newStatus);
        return ResponseEntity.ok(TeamResponse.from(teamRepository.save(team)));
    }

    // DELETE TEAM (also removes its members). Blocked if projects still belong to the team.
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + id);
        }

        if (projectRepository.existsByTeam_TeamId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete team: it still has projects. Delete the projects first.");
        }

        teamMemberRepository.deleteAll(teamMemberRepository.findByTeam_TeamId(id));
        teamRepository.delete(team);
        return ResponseEntity.noContent().build();
    }
}
