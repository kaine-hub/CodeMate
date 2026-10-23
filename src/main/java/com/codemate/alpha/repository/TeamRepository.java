package com.codemate.alpha.repository;

import com.codemate.alpha.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByLeader_UserId(Long userId);

    List<Team> findByRecruitmentStatusIgnoreCase(String recruitmentStatus);
}
