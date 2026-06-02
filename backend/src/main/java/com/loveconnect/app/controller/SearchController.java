package com.loveconnect.app.controller;

import com.loveconnect.app.dto.MatchResponse;
import com.loveconnect.app.dto.SearchRequest;
import com.loveconnect.app.entity.Gender;
import com.loveconnect.app.service.CurrentUserService;
import com.loveconnect.app.service.MatchService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final CurrentUserService currentUserService;
    private final MatchService matchService;

    public SearchController(CurrentUserService currentUserService, MatchService matchService) {
        this.currentUserService = currentUserService;
        this.matchService = matchService;
    }

    @GetMapping
    public List<MatchResponse> search(Authentication authentication,
                                      @RequestParam(required = false) Integer minAge,
                                      @RequestParam(required = false) Integer maxAge,
                                      @RequestParam(required = false) Gender gender,
                                      @RequestParam(required = false) String city,
                                      @RequestParam(required = false) String interest) {
        return matchService.search(currentUserService.get(authentication), new SearchRequest(minAge, maxAge, gender, city, interest));
    }

    @GetMapping("/recommendations")
    public List<MatchResponse> recommendations(Authentication authentication) {
        return matchService.recommendations(currentUserService.get(authentication));
    }
}


