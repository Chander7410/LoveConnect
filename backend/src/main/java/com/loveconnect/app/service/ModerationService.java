package com.loveconnect.app.service;

import com.loveconnect.app.exception.BadRequestException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ModerationService {
    private static final List<String> BLOCKED_TERMS = Arrays.asList("scam", "fraud", "escort", "onlyfans", "telegram");
    private static final Pattern LINK_PATTERN = Pattern.compile("https?://|www\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_PATTERN = Pattern.compile("(.)\\1{7,}");

    public void validateUserContent(String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (BLOCKED_TERMS.stream().anyMatch(normalized::contains)) {
            throw new BadRequestException("Content moderation blocked unsafe profile or message text.");
        }
        if (REPEATED_PATTERN.matcher(value).find()) {
            throw new BadRequestException("Spam detection blocked repeated character content.");
        }
    }

    public void validateMessage(String value) {
        validateUserContent(value);
        if (value != null && LINK_PATTERN.matcher(value).find()) {
            throw new BadRequestException("Spam detection blocked links in chat messages.");
        }
    }

    public int fakeProfileScore(String bio, String profilePictureUrl, int photoCount, int interestCount) {
        int score = 0;
        if (bio == null || bio.trim().length() < 20) score += 25;
        if (profilePictureUrl == null || profilePictureUrl.trim().isEmpty()) score += 30;
        if (photoCount == 0) score += 20;
        if (interestCount == 0) score += 15;
        if (bio != null && LINK_PATTERN.matcher(bio).find()) score += 25;
        return Math.min(100, score);
    }

    public int reportRiskScore(String reason, String details) {
        String text = ((reason == null ? "" : reason) + " " + (details == null ? "" : details)).toLowerCase(Locale.ROOT);
        int score = 35;
        if (text.contains("fake") || text.contains("scam")) score += 30;
        if (text.contains("abuse") || text.contains("harass") || text.contains("threat")) score += 25;
        if (text.contains("spam")) score += 20;
        return Math.min(100, score);
    }
}
