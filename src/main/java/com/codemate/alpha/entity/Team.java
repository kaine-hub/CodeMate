
package com.codemate.alpha.entity.TEAM;

import com.codemate.alpha.entity.USER.User;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teamId;

    @ManyToOne
    @JoinColumn(name = "leader_user_id", nullable = false)
    private User leader;

    @Column(nullable = false, length = 100)
    private String teamName;

    @Column(nullable = false)
    private Short maxMembers;

    @Column(nullable = false)
    private Short currentMembers;

    @Column(length = 20)
    private String recruitmentStatus;

    public Team() {
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public User getLeader() {
        return leader;
    }

    public void setLeader(User leader) {
        this.leader = leader;
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
}