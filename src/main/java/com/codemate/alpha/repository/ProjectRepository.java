package com.codemate.alpha.repository;

import com.codemate.alpha.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}