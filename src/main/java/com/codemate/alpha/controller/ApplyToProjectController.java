package com.codemate.alpha.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codemate.alpha.entity.ApplyToProject;
import com.codemate.alpha.entity.Project;
import com.codemate.alpha.entity.Team;
import com.codemate.alpha.entity.TeamMember;
import com.codemate.alpha.entity.User;
import com.codemate.alpha.repository.ApplyToProjectRepository;
import com.codemate.alpha.repository.ProjectRepository;
import com.codemate.alpha.repository.TeamMemberRepository;
import com.codemate.alpha.repository.TeamRepository;
import com.codemate.alpha.repository.UserRepository;

@RestController
@RequestMapping("/api/applications")
public class ApplyToProjectController {

    private final ApplyToProjectRepository applicationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public ApplyToProjectController(
            ApplyToProjectRepository applicationRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository) {

        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    // APPLY TO A PROJECT
    @PostMapping
    public ResponseEntity<?> applyToProject(@RequestBody ApplyToProject application) {

        if (application.getUser() == null || application.getProject() == null) {
            return ResponseEntity.badRequest().body("User and project are required");
        }

        Long userId = application.getUser().getUserId();
        Long projectId = application.getProject().getProjectId();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        // User cannot apply to their own project
        if (project.getPostedByUser().getUserId().equals(userId)) {
            return ResponseEntity.badRequest()
                    .body("You cannot apply to your own project");
        }

        // Check if the user has already applied
        for (ApplyToProject existingApplication : applicationRepository.findAll()) {

            if (existingApplication.getUser().getUserId().equals(userId)
                    && existingApplication.getProject().getProjectId().equals(projectId)
                    && "PENDING".equals(existingApplication.getApplicationStatus())) {

                return ResponseEntity.badRequest()
                        .body("You already have a pending application for this project");
            }
        }

        application.setUser(user);
        application.setProject(project);
        application.setApplicationStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());

        ApplyToProject savedApplication = applicationRepository.save(application);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedApplication);
    }

    // GET APPLICATIONS FOR A PROJECT
    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getApplicationsForProject(@PathVariable Long projectId) {

        Project project = projectRepository.findById(projectId).orElse(null);

        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        List<ApplyToProject> applications = applicationRepository.findAll()
                .stream()
                .filter(application ->
                        application.getProject().getProjectId().equals(projectId))
                .toList();

        return ResponseEntity.ok(applications);
    }

    // GET APPLICATIONS OF A USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getApplicationsForUser(@PathVariable Long userId) {

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        List<ApplyToProject> applications = applicationRepository.findAll()
                .stream()
                .filter(application ->
                        application.getUser().getUserId().equals(userId))
                .toList();

        return ResponseEntity.ok(applications);
    }

    // ACCEPT APPLICATION
    @PatchMapping("/{applicationId}/accept")
    public ResponseEntity<?> acceptApplication(
            @PathVariable Long applicationId,
            @RequestParam Long leaderId) {

        ApplyToProject application = applicationRepository
                .findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Application not found");
        }

        if (!"PENDING".equals(application.getApplicationStatus())) {
            return ResponseEntity.badRequest()
                    .body("Application has already been processed");
        }

        Project project = application.getProject();
        Team team = project.getTeam();

        // SECURITY CHECK
        if (!team.getLeader().getUserId().equals(leaderId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the team leader can accept this application");
        }

        // Check team capacity
        if (team.getCurrentMembers() >= team.getMaxMembers()) {
            return ResponseEntity.badRequest()
                    .body("Team is already full");
        }

        User applicant = application.getUser();

        // Check if applicant is already a team member
        for (TeamMember member : teamMemberRepository.findAll()) {

            if (member.getTeam().getTeamId().equals(team.getTeamId())
                    && member.getUser().getUserId().equals(applicant.getUserId())) {

                return ResponseEntity.badRequest()
                        .body("User is already a member of this team");
            }
        }

        // Create TeamMember
        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(applicant);
        teamMember.setJoinedAt(LocalDateTime.now());

        teamMemberRepository.save(teamMember);

        // Update team member count
        team.setCurrentMembers(
                (short) (team.getCurrentMembers() + 1)
        );

        teamRepository.save(team);

        // Update application
        application.setApplicationStatus("ACCEPTED");

        applicationRepository.save(application);

        return ResponseEntity.ok(application);
    }

    // REJECT APPLICATION
    @PatchMapping("/{applicationId}/reject")
    public ResponseEntity<?> rejectApplication(
            @PathVariable Long applicationId,
            @RequestParam Long leaderId) {

        ApplyToProject application = applicationRepository
                .findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Application not found");
        }

        if (!"PENDING".equals(application.getApplicationStatus())) {
            return ResponseEntity.badRequest()
                    .body("Application has already been processed");
        }

        Project project = application.getProject();
        Team team = project.getTeam();

        // SECURITY CHECK
        if (!team.getLeader().getUserId().equals(leaderId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the team leader can reject this application");
        }

        application.setApplicationStatus("REJECTED");

        applicationRepository.save(application);

        return ResponseEntity.ok(application);
    }
}