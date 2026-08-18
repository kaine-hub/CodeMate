package com.codemate.alpha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codemate.alpha.entity.APPLY.ApplyToProject;

public interface ApplyToProjectRepository extends JpaRepository<ApplyToProject, Long> {
}