package com.codemate.alpha.repository;

import com.codemate.alpha.entity.USER.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
}