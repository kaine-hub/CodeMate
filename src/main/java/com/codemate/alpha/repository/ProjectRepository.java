package com.codemate.alpha.repository;

import com.codemate.alpha.entity.PROJECT.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}