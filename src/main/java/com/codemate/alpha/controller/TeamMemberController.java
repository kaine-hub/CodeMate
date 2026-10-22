package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Team;
import com.codemate.alpha.entity.TeamMember;
import com.codemate.alpha.entity.User;
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
@RequestMapping("/api/team-members")
public class TeamMemberController {
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamMemberController(TeamMemberRepository teamMemberRepository, TeamRepository teamRepository, UserRepository userRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
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
    public ResponseEntity<List<Map<String, Object>>> getAllTeamMembers() {
        return ResponseEntity.ok(teamMemberRepository.findAll().stream().map(this::memberData).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTeamMemberById(@PathVariable Long id) {
        TeamMember member = teamMemberRepository.findById(id).orElse(null);
        return member == null ? ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team member not found with id: " + id)
                : ResponseEntity.ok(memberData(member));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Map<String, Object>>> getMembersByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamMemberRepository.findByTeam_TeamId(teamId).stream().map(this::memberData).toList());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getMembershipsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(teamMemberRepository.findByUser_UserId(userId).stream().map(this::memberData).toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> addMember(@RequestParam Long teamId, @RequestParam Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + teamId);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with id: " + userId);
        if (teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(teamId, userId)) return ResponseEntity.status(HttpStatus.CONFLICT).body("User is already a member of this team");
        if (team.getCurrentMembers() >= team.getMaxMembers()) return ResponseEntity.badRequest().body("Team is already full");
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        TeamMember saved = teamMemberRepository.save(member);
        team.setCurrentMembers((short) (team.getCurrentMembers() + 1));
        if (team.getCurrentMembers() >= team.getMaxMembers()) team.setRecruitmentStatus("CLOSED");
        teamRepository.save(team);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberData(saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> removeMember(@PathVariable Long id) {
        TeamMember member = teamMemberRepository.findById(id).orElse(null);
        if (member == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team member not found with id: " + id);
        return removeInternal(member);
    }

    @DeleteMapping("/team/{teamId}/user/{userId}")
    @Transactional
    public ResponseEntity<?> removeMemberByTeamAndUser(@PathVariable Long teamId, @PathVariable Long userId) {
        TeamMember member = teamMemberRepository.findByTeam_TeamIdAndUser_UserId(teamId, userId).orElse(null);
        if (member == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("This user is not a member of the team");
        return removeInternal(member);
    }

    private ResponseEntity<?> removeInternal(TeamMember member) {
        Team team = member.getTeam();
        if (team.getLeader().getUserId().equals(member.getUser().getUserId())) return ResponseEntity.badRequest().body("The team leader cannot be removed from the team");
        teamMemberRepository.delete(member);
        int remaining = Math.max(1, team.getCurrentMembers() - 1);
        team.setCurrentMembers((short) remaining);
        if (team.getCurrentMembers() < team.getMaxMembers() && "CLOSED".equalsIgnoreCase(team.getRecruitmentStatus())) team.setRecruitmentStatus("OPEN");
        teamRepository.save(team);
        return ResponseEntity.noContent().build();
    }
}
