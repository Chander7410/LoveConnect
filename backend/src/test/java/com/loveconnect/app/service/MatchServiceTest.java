package com.loveconnect.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.loveconnect.app.entity.Profile;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.repository.LikeRepository;
import com.loveconnect.app.repository.MatchRepository;
import com.loveconnect.app.repository.UserRepository;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock UserRepository userRepository;
    @Mock LikeRepository likeRepository;
    @Mock MatchRepository matchRepository;
    @Mock NotificationService notificationService;
    @InjectMocks MatchService matchService;

    @Test
    void recommendationsRankUsersWithCommonInterests() {
        User current = user(1L, "Ava", 28, "Pune", new HashSet<>(Arrays.asList("music", "travel")));
        User candidate = user(2L, "Mia", 29, "Pune", new HashSet<>(Arrays.asList("music", "fitness")));
        when(userRepository.findAll()).thenReturn(Arrays.asList(current, candidate));

        List<com.loveconnect.app.dto.MatchResponse> results = matchService.recommendations(current);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCommonInterests()).containsExactly("music");
        assertThat(results.get(0).getMatchScore()).isGreaterThanOrEqualTo(30);
    }

    private User user(Long id, String name, int age, String location, Set<String> interests) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setMobileNumber("99999999" + id);
        user.setAge(age);
        user.setLocation(location);
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setInterests(interests);
        user.setProfile(profile);
        return user;
    }
}


