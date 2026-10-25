package com.codemate.alpha.response;

import com.codemate.alpha.entity.Team;

public class TeamResponse {

    private Long teamId;
    private Long leaderUserId;
    private String leaderName;
    private String teamName;
    private Short maxMembers;
    private Short currentMembers;
    private String recruitmentStatus;

    public TeamResponse() {
    }

    public TeamResponse(Long teamId, Long leaderUserId, String leaderName,
                        String teamName, Short maxMembers, Short currentMembers,
                        String recruitmentStatus) {
        this.teamId = teamId;
        this.leaderUserId = leaderUserId;
        this.leaderName = leaderName;
        this.teamName = teamName;
        this.maxMembers = maxMembers;
        this.currentMembers = currentMembers;
        this.recruitmentStatus = recruitmentStatus;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getLeaderUserId() {
        return leaderUserId;
    }

    public void setLeaderUserId(Long leaderUserId) {
        this.leaderUserId = leaderUserId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Short getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Short maxMembers) {
        this.maxMembers = maxMembers;
    }

    public Short getCurrentMembers() {
        return currentMembers;
    }

    public void setCurrentMembers(Short currentMembers) {
        this.currentMembers = currentMembers;
    }

    public String getRecruitmentStatus() {
        return recruitmentStatus;
    }

    public void setRecruitmentStatus(String recruitmentStatus) {
        this.recruitmentStatus = recruitmentStatus;
    }

    public static TeamResponse from(Team team) {
        return new TeamResponse(
            team.getTeamId(),
            team.getLeader().getUserId(),
            team.getLeader().getFirstName() + " " + team.getLeader().getLastName(),
            team.getTeamName(),
            team.getMaxMembers(),
            team.getCurrentMembers(),
            team.getRecruitmentStatus()
        );
    }
}
