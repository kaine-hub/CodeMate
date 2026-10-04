package com.codemate.alpha.repository;

import com.codemate.alpha.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    boolean existsByWebsite(String website);

    Optional<Competition> findByWebsite(String website);
}
