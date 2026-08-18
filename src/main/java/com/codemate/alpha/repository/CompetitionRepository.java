package com.codemate.alpha.repository;

import com.codemate.alpha.entity.PROJECT.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
}