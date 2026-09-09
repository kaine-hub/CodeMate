package com.codemate.alpha.response;

import com.codemate.alpha.entity.Skill;

public class SkillResponse {

    private Long skillId;
    private String skillName;
    private String description;

    public SkillResponse() {
    }

    public SkillResponse(Long skillId, String skillName, String description) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.description = description;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(
            skill.getSkillId(),
            skill.getSkillName(),
            skill.getDescription()
        );
    }
}