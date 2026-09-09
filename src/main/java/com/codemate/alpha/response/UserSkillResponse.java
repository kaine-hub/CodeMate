package com.codemate.alpha.response;

import com.codemate.alpha.entity.UserSkill;

public class UserSkillResponse {

    private Long userSkillId;
    private Long userId;
    private Long skillId;
    private String skillName;
    private String proficiencyLevel;

    public UserSkillResponse() {
    }

    public UserSkillResponse(Long userSkillId, Long userId, Long skillId,
                             String skillName, String proficiencyLevel) {
        this.userSkillId = userSkillId;
        this.userId = userId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.proficiencyLevel = proficiencyLevel;
    }

    public Long getUserSkillId() {
        return userSkillId;
    }

    public void setUserSkillId(Long userSkillId) {
        this.userSkillId = userSkillId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(String proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public static UserSkillResponse from(UserSkill userSkill) {
        return new UserSkillResponse(
            userSkill.getUserSkillId(),
            userSkill.getUser().getUserId(),
            userSkill.getSkill().getSkillId(),
            userSkill.getSkill().getSkillName(),
            userSkill.getProficiencyLevel()
        );
    }
}