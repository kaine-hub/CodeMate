package com.codemate.alpha.repository;

import com.codemate.alpha.entity.USER.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
}