package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Team;
import com.codemate.alpha.entity.TeamMember;
import com.codemate.alpha.entity.User;
import com.codemate.alpha.repository.TeamMemberRepository;
import com.codemate.alpha.repository.TeamRepository;
import com.codemate.alpha.repository.UserRepository;
import com.codemate.alpha.response.TeamMemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/team-members")
public class TeamMemberController {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamMemberController(TeamMemberRepository teamMemberRepository,
                                TeamRepository teamRepository,
                                UserRepository userRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    // GET ALL TEAM MEMBERS
    @GetMapping
    public ResponseEntity<List<TeamMemberResponse>> getAllTeamMembers() {
        return ResponseEntity.ok(
                teamMemberRepository.findAll()
                        .stream().map(TeamMemberResponse::from).toList());
    }

    // GET TEAM MEMBER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTeamMemberById(@PathVariable Long id) {
        return teamMemberRepository.findById(id)
                .<ResponseEntity<?>>map(tm -> ResponseEntity.ok(TeamMemberResponse.from(tm)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Team member not found with id: " + id));
    }

    // GET MEMBERS OF A TEAM
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<TeamMemberResponse>> getMembersByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(
                teamMemberRepository.findByTeam_TeamId(teamId)
                        .stream().map(TeamMemberResponse::from).toList());
    }

    // GET MEMBERSHIPS OF A USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TeamMemberResponse>> getMembershipsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(
                teamMemberRepository.findByUser_UserId(userId)
                        .stream().map(TeamMemberResponse::from).toList());
    }

    // ADD MEMBER TO TEAM: POST /api/team-members?teamId=1&userId=2
    @PostMapping
    @Transactional
    public ResponseEntity<?> addMember(@RequestParam Long teamId, @RequestParam Long userId) {

        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with id: " + teamId);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with id: " + userId);
        }

        if (teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(teamId, userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User is already a member of this team");
        }

        if (team.getCurrentMembers() >= team.getMaxMembers()) {
            return ResponseEntity.badRequest().body("Team is already full");
        }

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        TeamMember saved = teamMemberRepository.save(member);

        team.setCurrentMembers((short) (team.getCurrentMembers() + 1));
        if (team.getCurrentMembers() >= team.getMaxMembers()) {
            team.setRecruitmentStatus("CLOSED");
        }
        teamRepository.save(team);

        return ResponseEntity.status(HttpStatus.CREATED).body(TeamMemberResponse.from(saved));
    }

    // REMOVE MEMBER BY teamMemberId. The team leader cannot be removed.
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> removeMember(@PathVariable Long id) {
        TeamMember member = teamMemberRepository.findById(id).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Team member not found with id: " + id);
        }
        return removeInternal(member);
    }

    // REMOVE / LEAVE BY TEAM AND USER: DELETE /api/team-members/team/1/user/2
    @DeleteMapping("/team/{teamId}/user/{userId}")
    @Transactional
    public ResponseEntity<?> removeMemberByTeamAndUser(@PathVariable Long teamId,
                                                       @PathVariable Long userId) {
        TeamMember member = teamMemberRepository
                .findByTeam_TeamIdAndUser_UserId(teamId, userId).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("This user is not a member of the team");
        }
        return removeInternal(member);
    }

    private ResponseEntity<?> removeInternal(TeamMember member) {
        Team team = member.getTeam();

        if (team.getLeader().getUserId().equals(member.getUser().getUserId())) {
            return ResponseEntity.badRequest()
                    .body("The team leader cannot be removed from the team");
        }

        teamMemberRepository.delete(member);

        int remaining = Math.max(1, team.getCurrentMembers() - 1);
        team.setCurrentMembers((short) remaining);
        teamRepository.save(team);

        return ResponseEntity.noContent().build();
    }
}
