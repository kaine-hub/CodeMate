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

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Set<String> PROJECT_STATUSES = Set.of("OPEN", "IN_PROGRESS", "COMPLETED"); // <- put your real
                                                                                                    // values here

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    private ResponseEntity<String> bad(String m) {
        return ResponseEntity.badRequest().body(m);
    }

    private ResponseEntity<String> conflict(String m) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(m);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank())
            return false;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && host != null && host.contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    // call only after isValidHttpUrl passed.
    // lowercases scheme+host, drops trailing slash and #fragment so formatting
    // can't dodge duplicate checks
    private static String normalizeUrl(String value) {
        URI u = URI.create(value.trim());
        String path = u.getRawPath() == null ? "" : u.getRawPath();
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        String out = u.getScheme().toLowerCase() + "://" + u.getRawAuthority().toLowerCase() + path;
        if (u.getRawQuery() != null)
            out += "?" + u.getRawQuery();
        return out;
    }

    // returns an error message, or null if the team may be linked to this
    // competition
    private String checkCompetitionFits(Competition competition, Team team) {
        if (competition.getRegistrationDeadline() != null
                && competition.getRegistrationDeadline().isBefore(LocalDate.now())) {
            return "Registration for this competition has closed";
        }
        Short limit = competition.getTeamSizeLimit();
        if (limit != null && team.getCurrentMembers() > limit) {
            return "Team has more members than this competition allows (" + limit + ")";
        }
        return null;
    }

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
            return bad("teamId is required");
        }
        Team team = teamRepository.findById(project.getTeam().getTeamId()).orElse(null);
        if (team == null) {
            return bad("Team not found with id: " + project.getTeam().getTeamId());
        }

        if (project.getPostedByUser() == null || project.getPostedByUser().getUserId() == null) {
            return bad("postedByUser id is required");
        }
        User postedByUser = userRepository.findById(project.getPostedByUser().getUserId()).orElse(null);
        if (postedByUser == null) {
            return bad("User not found with id: " + project.getPostedByUser().getUserId());
        }

        // title
        String title = project.getProjectTitle() == null ? "" : project.getProjectTitle().trim();
        if (title.isEmpty() || title.length() > 150) {
            return bad("Project title must be 1-150 characters");
        }

        // status (optional)
        String status = null;
        if (!isBlank(project.getProjectStatus())) {
            status = project.getProjectStatus().trim().toUpperCase();
            if (!PROJECT_STATUSES.contains(status)) {
                return bad("Invalid project status. Allowed: " + PROJECT_STATUSES);
            }
        }

        // links (optional, but valid + unique when present)
        String githubLink = null;
        if (!isBlank(project.getGithubLink())) {
            if (!isValidHttpUrl(project.getGithubLink()))
                return bad("githubLink must be a valid http(s) URL");
            githubLink = normalizeUrl(project.getGithubLink());
            if (githubLink.length() > 255)
                return bad("githubLink must be at most 255 characters");
            if (projectRepository.existsByGithubLink(githubLink))
                return conflict("This GitHub link is already used by another project");
        }
        String demoLink = null;
        if (!isBlank(project.getDemoLink())) {
            if (!isValidHttpUrl(project.getDemoLink()))
                return bad("demoLink must be a valid http(s) URL");
            demoLink = normalizeUrl(project.getDemoLink());
            if (demoLink.length() > 255)
                return bad("demoLink must be at most 255 characters");
            if (projectRepository.existsByDemoLink(demoLink))
                return conflict("This demo link is already used by another project");
        }

        // standalone projects (no competition) don't need githubLink/demoLink;
        // projects submitted to a competition must have them filled in
        Competition competition = null;
        if (project.getCompetition() != null && project.getCompetition().getCompetitionId() != null) {
            competition = competitionRepository.findById(project.getCompetition().getCompetitionId()).orElse(null);
            if (competition == null) {
                return bad("Competition not found with id: " + project.getCompetition().getCompetitionId());
            }
            String problem = checkCompetitionFits(competition, team);
            if (problem != null)
                return bad(problem);
            if (githubLink == null || demoLink == null) {
                return bad("githubLink and demoLink are required when a project is linked to a competition");
            }
        }

        project.setProjectId(null); // a client must not be able to overwrite an existing project via POST
        project.setProjectTitle(title);
        project.setProjectStatus(status);
        project.setGithubLink(githubLink);
        project.setDemoLink(demoLink);
        project.setTeam(team);
        project.setPostedByUser(postedByUser);
        project.setCompetition(competition);
        project.setCreatedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(projectRepository.save(project));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody Project updated) {

        Project existing = projectRepository.findById(id).orElse(null);
        if (existing == null)
            return ResponseEntity.notFound().build();

        // title
        String title = existing.getProjectTitle();
        if (updated.getProjectTitle() != null) {
            title = updated.getProjectTitle().trim();
            if (title.isEmpty() || title.length() > 150)
                return bad("Project title must be 1-150 characters");
        }

        // status
        String status = existing.getProjectStatus();
        if (updated.getProjectStatus() != null) {
            status = updated.getProjectStatus().trim().toUpperCase();
            if (!PROJECT_STATUSES.contains(status))
                return bad("Invalid project status. Allowed: " + PROJECT_STATUSES);
        }

        // github link: valid + normalized + unique among OTHER projects
        String githubLink = existing.getGithubLink();
        if (updated.getGithubLink() != null) {
            if (updated.getGithubLink().isBlank()) {
                githubLink = null;
            } else {
                if (!isValidHttpUrl(updated.getGithubLink()))
                    return bad("githubLink must be a valid http(s) URL");
                githubLink = normalizeUrl(updated.getGithubLink());
                if (githubLink.length() > 255)
                    return bad("githubLink must be at most 255 characters");
                if (projectRepository.existsByGithubLinkAndProjectIdNot(githubLink, id))
                    return conflict("This GitHub link is already used by another project");
            }
        }

        // demo link: same rules
        String demoLink = existing.getDemoLink();
        if (updated.getDemoLink() != null) {
            if (updated.getDemoLink().isBlank()) {
                demoLink = null;
            } else {
                if (!isValidHttpUrl(updated.getDemoLink()))
                    return bad("demoLink must be a valid http(s) URL");
                demoLink = normalizeUrl(updated.getDemoLink());
                if (demoLink.length() > 255)
                    return bad("demoLink must be at most 255 characters");
                if (projectRepository.existsByDemoLinkAndProjectIdNot(demoLink, id))
                    return conflict("This demo link is already used by another project");
            }
        }

        // competition: only re-checked when a DIFFERENT one is being linked.
        // competition omitted/null in the body = untouched; use DELETE
        // /{id}/competition to unlink
        Competition competition = existing.getCompetition();
        if (updated.getCompetition() != null && updated.getCompetition().getCompetitionId() != null) {
            Long newId = updated.getCompetition().getCompetitionId();
            competition = competitionRepository.findById(newId).orElse(null);
            if (competition == null)
                return bad("Competition not found with id: " + newId);

            boolean changed = existing.getCompetition() == null
                    || !existing.getCompetition().getCompetitionId().equals(newId);
            if (changed) {
                String problem = checkCompetitionFits(competition, existing.getTeam());
                if (problem != null)
                    return bad(problem);
            }
        }
        if (competition != null && (githubLink == null || demoLink == null)) {
            return bad("githubLink and demoLink are required when a project is linked to a competition");
        }

        // everything passed, now apply
        existing.setProjectTitle(title);
        existing.setProjectStatus(status);
        existing.setGithubLink(githubLink);
        existing.setDemoLink(demoLink);
        existing.setCompetition(competition);
        if (updated.getDescription() != null)
            existing.setDescription(updated.getDescription());

        return ResponseEntity.ok(projectRepository.save(existing));
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
}