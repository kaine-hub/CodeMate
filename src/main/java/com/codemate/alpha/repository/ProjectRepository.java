package com.codemate.alpha.repository;

import com.codemate.alpha.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByTeam_TeamId(Long teamId);
    boolean existsByGithubLink(String githubLink);
    boolean existsByDemoLink(String demoLink);
    boolean existsByGithubLinkAndProjectIdNot(String githubLink, Long projectId);
    boolean existsByDemoLinkAndProjectIdNot(String demoLink, Long projectId);
}