package com.loveconnect.app.controller;

import com.loveconnect.app.dto.LikeRequest;
import com.loveconnect.app.dto.MatchResponse;
import com.loveconnect.app.service.CurrentUserService;
import com.loveconnect.app.service.MatchService;
import javax.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/likes")
public class LikeController {
    private final CurrentUserService currentUserService;
    private final MatchService matchService;

    public LikeController(CurrentUserService currentUserService, MatchService matchService) {
        this.currentUserService = currentUserService;
        this.matchService = matchService;
    }

    @PostMapping
    public MatchResponse react(Authentication authentication, @Valid @RequestBody LikeRequest request) {
        return matchService.react(currentUserService.get(authentication), request);
    }

    @GetMapping("/received")
    public List<MatchResponse> received(Authentication authentication) {
        return matchService.receivedLikes(currentUserService.get(authentication));
    }

    @GetMapping("/matches")
    public List<MatchResponse> matches(Authentication authentication) {
        return matchService.myMatches(currentUserService.get(authentication));
    }
}


