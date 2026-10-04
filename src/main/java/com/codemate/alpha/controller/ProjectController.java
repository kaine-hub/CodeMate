package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Competition;
import com.codemate.alpha.entity.Project;
import com.codemate.alpha.entity.Team;
import com.codemate.alpha.entity.User;
import com.codemate.alpha.repository.CompetitionRepository;
import com.codemate.alpha.repository.ProjectRepository;
import com.codemate.alpha.repository.TeamRepository;
import com.codemate.alpha.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Project>> getProjectsByTeam(@PathVariable Long teamId) {
        List<Project> projects = projectRepository.findAll()
                .stream()
                .filter(p -> p.getTeam() != null && teamId.equals(p.getTeam().getTeamId()))
                .toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<List<Project>> getProjectsByCompetition(@PathVariable Long competitionId) {
        List<Project> projects = projectRepository.findAll()
                .stream()
                .filter(p -> p.getCompetition() != null && competitionId.equals(p.getCompetition().getCompetitionId()))
                .toList();
        return ResponseEntity.ok(projects);
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Project project) {
        if (project.getTeam() == null || project.getTeam().getTeamId() == null) {
            return ResponseEntity.badRequest().body("teamId is required");
        }
        Team team = teamRepository.findById(project.getTeam().getTeamId()).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body("Team not found with id: " + project.getTeam().getTeamId());
        }

        if (project.getPostedByUser() == null || project.getPostedByUser().getUserId() == null) {
            return ResponseEntity.badRequest().body("postedByUser id is required");
        }
        User postedByUser = userRepository.findById(project.getPostedByUser().getUserId()).orElse(null);
        if (postedByUser == null) {
            return ResponseEntity.badRequest()
                    .body("User not found with id: " + project.getPostedByUser().getUserId());
        }

        // standalone projects (no competition) don't need githubLink/demoLink;
        // projects submitted to a competition must have them filled in
        boolean linkedToCompetition = project.getCompetition() != null && project.getCompetition().getCompetitionId() != null;
        Competition competition = null;
        if (linkedToCompetition) {
            competition = competitionRepository.findById(project.getCompetition().getCompetitionId()).orElse(null);
            if (competition == null) {
                return ResponseEntity.badRequest()
                        .body("Competition not found with id: " + project.getCompetition().getCompetitionId());
            }
            if (isBlank(project.getGithubLink()) || isBlank(project.getDemoLink())) {
                return ResponseEntity.badRequest()
                        .body("githubLink and demoLink are required when a project is linked to a competition");
            }
        }

        project.setTeam(team);
        project.setPostedByUser(postedByUser);
        project.setCompetition(competition);
        project.setCreatedAt(LocalDateTime.now());

        Project saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody Project updatedProject) {
        return projectRepository.findById(id)
                .map(existing -> {
                    if (updatedProject.getCompetition() != null && updatedProject.getCompetition().getCompetitionId() != null) {
                        Competition competition = competitionRepository
                                .findById(updatedProject.getCompetition().getCompetitionId())
                                .orElse(null);
                        if (competition == null) {
                            return ResponseEntity.badRequest()
                                    .body("Competition not found with id: " + updatedProject.getCompetition().getCompetitionId());
                        }
                        existing.setCompetition(competition);
                    }
                    // competition not touched here if omitted/null in the request body —
                    // use DELETE /api/projects/{id}/competition below to unlink one explicitly

                    String githubLink = updatedProject.getGithubLink() != null ? updatedProject.getGithubLink() : existing.getGithubLink();
                    String demoLink = updatedProject.getDemoLink() != null ? updatedProject.getDemoLink() : existing.getDemoLink();

                    if (existing.getCompetition() != null && (isBlank(githubLink) || isBlank(demoLink))) {
                        return ResponseEntity.badRequest()
                                .body("githubLink and demoLink are required when a project is linked to a competition");
                    }

                    existing.setProjectTitle(updatedProject.getProjectTitle());
                    existing.setDescription(updatedProject.getDescription());
                    existing.setGithubLink(githubLink);
                    existing.setDemoLink(demoLink);
                    existing.setProjectStatus(updatedProject.getProjectStatus());

                    Project saved = projectRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        if (!projectRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/competition")
    public ResponseEntity<?> unlinkCompetition(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(existing -> {
                    existing.setCompetition(null);
                    Project saved = projectRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
