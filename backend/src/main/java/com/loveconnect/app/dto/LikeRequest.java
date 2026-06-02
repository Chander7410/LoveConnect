package com.loveconnect.app.dto;

import javax.validation.constraints.NotNull;

public class LikeRequest {
    @NotNull private Long targetUserId;
    private boolean liked;
    private boolean superLike;

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isSuperLike() { return superLike; }
    public void setSuperLike(boolean superLike) { this.superLike = superLike; }
}

