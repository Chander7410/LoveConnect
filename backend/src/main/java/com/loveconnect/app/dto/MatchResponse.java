package com.loveconnect.app.dto;

import java.util.Set;

public class MatchResponse {
    private UserResponse user;
    private int matchScore;
    private Set<String> commonInterests;

    public MatchResponse() {}
    public MatchResponse(UserResponse user, int matchScore, Set<String> commonInterests) {
        this.user = user; this.matchScore = matchScore; this.commonInterests = commonInterests;
    }
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    public Set<String> getCommonInterests() { return commonInterests; }
    public void setCommonInterests(Set<String> commonInterests) { this.commonInterests = commonInterests; }
}

