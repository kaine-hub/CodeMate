package com.codemate.alpha.response;

import com.codemate.alpha.entity.TeamMember;

import java.time.LocalDateTime;

public class TeamMemberResponse {

    private Long teamMemberId;
    private Long teamId;
    private String teamName;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private boolean leader;
    private LocalDateTime joinedAt;

    public TeamMemberResponse() {
    }

    public TeamMemberResponse(Long teamMemberId, Long teamId, String teamName,
                              Long userId, String firstName, String lastName,
                              String email, boolean leader, LocalDateTime joinedAt) {
        this.teamMemberId = teamMemberId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.leader = leader;
        this.joinedAt = joinedAt;
    }

    public Long getTeamMemberId() {
        return teamMemberId;
    }

    public void setTeamMemberId(Long teamMemberId) {
        this.teamMemberId = teamMemberId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public static TeamMemberResponse from(TeamMember member) {
        boolean isLeader = member.getTeam().getLeader().getUserId()
                .equals(member.getUser().getUserId());
        return new TeamMemberResponse(
            member.getTeamMemberId(),
            member.getTeam().getTeamId(),
            member.getTeam().getTeamName(),
            member.getUser().getUserId(),
            member.getUser().getFirstName(),
            member.getUser().getLastName(),
            member.getUser().getEmail(),
            isLeader,
            member.getJoinedAt()
        );
    }
}
