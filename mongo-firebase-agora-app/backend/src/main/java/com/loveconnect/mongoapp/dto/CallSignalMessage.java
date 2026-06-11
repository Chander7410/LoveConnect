package com.loveconnect.mongoapp.dto;

import com.loveconnect.mongoapp.model.CallType;
import java.util.Map;

public record CallSignalMessage(
    String callId,
    String receiverId,
    String receiverUid,
    CallType callType,
    Map<String, Object> payload
) {
}
