package com.codemate.alpha.repository;

import com.codemate.alpha.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByUser_UserId(Long userId);

    List<UserSkill> findBySkill_SkillId(Long skillId);

    boolean existsByUser_UserIdAndSkill_SkillId(Long userId, Long skillId);
}