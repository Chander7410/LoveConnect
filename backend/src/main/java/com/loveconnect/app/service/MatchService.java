package com.loveconnect.app.service;

import com.loveconnect.app.dto.LikeRequest;
import com.loveconnect.app.dto.MatchResponse;
import com.loveconnect.app.dto.SearchRequest;
import com.loveconnect.app.entity.LikeAction;
import com.loveconnect.app.entity.Match;
import com.loveconnect.app.entity.NotificationType;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.exception.BadRequestException;
import com.loveconnect.app.exception.ResourceNotFoundException;
import com.loveconnect.app.repository.LikeRepository;
import com.loveconnect.app.repository.MatchRepository;
import com.loveconnect.app.repository.UserRepository;
import com.loveconnect.app.util.Mapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final MatchRepository matchRepository;
    private final NotificationService notificationService;
    private final SafetyService safetyService;

    public MatchService(UserRepository userRepository, LikeRepository likeRepository,
                        MatchRepository matchRepository, NotificationService notificationService,
                        SafetyService safetyService) {
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.matchRepository = matchRepository;
        this.notificationService = notificationService;
        this.safetyService = safetyService;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> search(User current, SearchRequest request) {
        final User managedCurrent = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(managedCurrent.getId()))
                .filter(u -> !u.isBlocked())
                .filter(u -> !safetyService.isBlockedBetween(managedCurrent.getId(), u.getId()))
                .filter(u -> request.getMinAge() == null || u.getAge() >= request.getMinAge())
                .filter(u -> request.getMaxAge() == null || u.getAge() <= request.getMaxAge())
                .filter(u -> request.getGender() == null || u.getGender() == request.getGender())
                .filter(u -> matchesCity(u, request.getCity()))
                .filter(u -> matchesInterest(u, request.getInterest()))
                .map(u -> response(managedCurrent, u))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> recommendations(User current) {
        final User managedCurrent = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(managedCurrent.getId()) && !u.isBlocked())
                .filter(u -> !safetyService.isBlockedBetween(managedCurrent.getId(), u.getId()))
                .map(u -> response(managedCurrent, u))
                .filter(r -> r.getMatchScore() >= 30)
                .sorted((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(20)
                .collect(Collectors.toList());
    }

    @Transactional
    public MatchResponse react(User current, LikeRequest request) {
        User managedCurrent = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        if (managedCurrent.getId().equals(request.getTargetUserId())) {
            throw new BadRequestException("You cannot react to your own profile");
        }
        User target = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));
        if (target.isBlocked() || safetyService.isBlockedBetween(managedCurrent.getId(), target.getId())) {
            throw new BadRequestException("This profile is unavailable");
        }
        LikeAction action = likeRepository.findByFromUserIdAndToUserId(managedCurrent.getId(), target.getId())
                .orElseGet(LikeAction::new);
        action.setFromUser(managedCurrent);
        action.setToUser(target);
        action.setLiked(request.isLiked() || request.isSuperLike());
        action.setSuperLike(request.isSuperLike());
        likeRepository.save(action);

        if (action.isLiked()) {
            notificationService.create(target,
                    request.isSuperLike() ? NotificationType.SUPER_LIKE : NotificationType.LIKE,
                    managedCurrent.getName() + (request.isSuperLike() ? " sent you a Super Like" : " liked your profile"));
            if (likeRepository.existsByFromUserIdAndToUserIdAndLikedTrue(target.getId(), managedCurrent.getId())) {
                createMatch(managedCurrent, target);
                notificationService.create(target, NotificationType.NEW_MATCH, "You matched with " + managedCurrent.getName());
                notificationService.create(managedCurrent, NotificationType.NEW_MATCH, "You matched with " + target.getName());
            }
        }
        return response(managedCurrent, target);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> receivedLikes(User current) {
        final User managedCurrent = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        return likeRepository.findByToUserIdAndLikedTrue(managedCurrent.getId()).stream()
                .filter(like -> !safetyService.isBlockedBetween(managedCurrent.getId(), like.getFromUser().getId()))
                .map(like -> response(managedCurrent, like.getFromUser()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MatchResponse> myMatches(User current) {
        final User managedCurrent = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        syncExistingMutualLikes(managedCurrent);
        return matchRepository.findByUserOneIdOrUserTwoId(managedCurrent.getId(), managedCurrent.getId()).stream()
                .map(match -> match.getUserOne().getId().equals(managedCurrent.getId()) ? match.getUserTwo() : match.getUserOne())
                .map(user -> response(managedCurrent, user))
                .collect(Collectors.toList());
    }

    private void syncExistingMutualLikes(User current) {
        likeRepository.findByToUserIdAndLikedTrue(current.getId()).stream()
                .filter(like -> likeRepository.existsByFromUserIdAndToUserIdAndLikedTrue(current.getId(), like.getFromUser().getId()))
                .forEach(like -> createMatch(current, like.getFromUser()));
    }

    private void createMatch(User a, User b) {
        Long one = Math.min(a.getId(), b.getId());
        Long two = Math.max(a.getId(), b.getId());
        if (!matchRepository.existsByUserOneIdAndUserTwoId(one, two)) {
            Match match = new Match();
            match.setUserOne(a.getId().equals(one) ? a : b);
            match.setUserTwo(a.getId().equals(two) ? a : b);
            match.setMatchScore(score(a, b));
            matchRepository.save(match);
        }
    }

    private MatchResponse response(User current, User candidate) {
        return new MatchResponse(Mapper.user(candidate), score(current, candidate), commonInterests(current, candidate));
    }

    private int score(User a, User b) {
        int score = 10;
        if (a.getLocation().equalsIgnoreCase(b.getLocation())) {
            score += 20;
        }
        int ageGap = Math.abs(a.getAge() - b.getAge());
        score += Math.max(0, 25 - ageGap * 3);
        score += Math.min(45, commonInterests(a, b).size() * 15);
        return Math.min(100, score);
    }

    private Set<String> commonInterests(User a, User b) {
        if (a.getProfile() == null || b.getProfile() == null) {
            return Collections.emptySet();
        }
        Set<String> common = new HashSet<>(a.getProfile().getInterests());
        common.retainAll(b.getProfile().getInterests());
        return common;
    }

    private boolean matchesCity(User user, String city) {
        if (city == null || city.trim().isEmpty()) {
            return true;
        }
        String candidateCity = user.getProfile() != null && user.getProfile().getCity() != null
                ? user.getProfile().getCity()
                : user.getLocation();
        return candidateCity != null && candidateCity.toLowerCase().contains(city.trim().toLowerCase());
    }

    private boolean matchesInterest(User user, String interest) {
        if (interest == null || interest.trim().isEmpty()) {
            return true;
        }
        if (user.getProfile() == null) {
            return false;
        }
        final String needle = interest.trim().toLowerCase();
        return user.getProfile().getInterests().stream()
                .anyMatch(value -> value != null && value.toLowerCase().contains(needle));
    }
}


