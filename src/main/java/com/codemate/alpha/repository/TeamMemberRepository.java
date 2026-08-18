package com.codemate.alpha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codemate.alpha.entity.TEAM.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
}