package com.loveconnect.app.util;

import com.loveconnect.app.dto.MessageResponse;
import com.loveconnect.app.dto.NotificationResponse;
import com.loveconnect.app.dto.ProfileResponse;
import com.loveconnect.app.dto.ReportResponse;
import com.loveconnect.app.dto.CallResponse;
import com.loveconnect.app.dto.UserResponse;
import com.loveconnect.app.entity.CallSession;
import com.loveconnect.app.entity.Message;
import com.loveconnect.app.entity.Notification;
import com.loveconnect.app.entity.Photo;
import com.loveconnect.app.entity.Profile;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.entity.UserReport;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Mapper {
    private Mapper() {}

    public static UserResponse user(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(),
                user.getGender(), user.getAge(), user.getLocation(), user.getProfilePictureUrl(),
                user.getRole(), user.isOnline(), user.isBlocked(), user.isVerified(), user.getFakeProfileScore());
    }

    public static ProfileResponse profile(Profile profile) {
        List<String> urls = profile.getPhotos().stream()
                .sorted(Comparator.comparing(Photo::isPrimaryPhoto).reversed())
                .map(Photo::getUrl)
                .collect(Collectors.toList());
        Set<String> interests = new LinkedHashSet<>(profile.getInterests());
        return new ProfileResponse(profile.getId(), user(profile.getUser()), profile.getBio(),
                profile.getEducation(), profile.getProfession(), profile.getCity(),
                interests, urls);
    }

    public static MessageResponse message(Message message) {
        return new MessageResponse(message.getId(), message.getSender().getId(), message.getReceiver().getId(),
                message.getContent(), message.getCreatedAt(), message.getReadAt());
    }

    public static NotificationResponse notification(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getMessage(),
                notification.isReadNotification(), notification.isPushDelivered(), notification.isEmailQueued(),
                notification.getCreatedAt());
    }

    public static ReportResponse report(UserReport report) {
        return new ReportResponse(report.getId(), user(report.getReporter()), user(report.getReportedUser()),
                report.getReason(), report.getDetails(), report.getStatus(), report.getRiskScore(),
                report.getCreatedAt());
    }

    public static CallResponse call(CallSession call) {
        return new CallResponse(call.getId(), user(call.getCaller()), user(call.getReceiver()), call.getType(),
                call.getStatus(), call.getStartedAt(), call.getEndedAt(), call.getDurationSeconds());
    }
}


