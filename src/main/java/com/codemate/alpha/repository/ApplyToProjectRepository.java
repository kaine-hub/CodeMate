package com.codemate.alpha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codemate.alpha.entity.ApplyToProject;

public interface ApplyToProjectRepository extends JpaRepository<ApplyToProject, Long> {
}