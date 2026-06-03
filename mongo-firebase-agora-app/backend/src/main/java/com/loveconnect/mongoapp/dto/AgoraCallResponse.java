package com.loveconnect.mongoapp.dto;

import com.loveconnect.mongoapp.model.CallSession;

public record AgoraCallResponse(
    CallSession call,
    String appId,
    String channelName,
    String token,
    String note
) {
}
