package com.codemate.alpha.repository;

import com.codemate.alpha.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    boolean existsBySkillNameIgnoreCase(String name);

    boolean existsBySkillNameIgnoreCaseAndSkillIdNot(String name, Long skillId);
}