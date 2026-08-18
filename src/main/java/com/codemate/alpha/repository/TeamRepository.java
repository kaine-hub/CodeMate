package com.codemate.alpha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codemate.alpha.entity.TEAM.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
}