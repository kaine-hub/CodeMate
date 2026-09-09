package com.codemate.alpha.response;

import com.codemate.alpha.entity.User;
import java.time.LocalDateTime;

public class UserResponse {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String bio;
    private String githubLink;
    private String linkedinLink;
    private String portfolioLink;
    private String profileImage;
    private String status;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(Long userId, String firstName, String lastName,
                        String email, String bio, String githubLink,
                        String linkedinLink, String portfolioLink,
                        String profileImage, String status,
                        LocalDateTime createdAt) {

        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.bio = bio;
        this.githubLink = githubLink;
        this.linkedinLink = linkedinLink;
        this.portfolioLink = portfolioLink;
        this.profileImage = profileImage;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getGithubLink() {
        return githubLink;
    }

    public void setGithubLink(String githubLink) {
        this.githubLink = githubLink;
    }

    public String getLinkedinLink() {
        return linkedinLink;
    }

    public void setLinkedinLink(String linkedinLink) {
        this.linkedinLink = linkedinLink;
    }

    public String getPortfolioLink() {
        return portfolioLink;
    }

    public void setPortfolioLink(String portfolioLink) {
        this.portfolioLink = portfolioLink;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getUserId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getBio(),
            user.getGithubLink(),
            user.getLinkedinLink(),
            user.getPortfolioLink(),
            user.getProfileImage(),
            user.getStatus(),
            user.getCreatedAt()
        );
    }
}