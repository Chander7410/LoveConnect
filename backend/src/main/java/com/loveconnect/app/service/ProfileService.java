package com.loveconnect.app.service;

import com.loveconnect.app.dto.ProfileRequest;
import com.loveconnect.app.dto.ProfileResponse;
import com.loveconnect.app.entity.Photo;
import com.loveconnect.app.entity.Profile;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.exception.ResourceNotFoundException;
import com.loveconnect.app.repository.PhotoRepository;
import com.loveconnect.app.repository.ProfileRepository;
import com.loveconnect.app.repository.UserRepository;
import com.loveconnect.app.util.Mapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final FileStorageService fileStorageService;
    private final ModerationService moderationService;
    private final NotificationService notificationService;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository,
                          PhotoRepository photoRepository, FileStorageService fileStorageService,
                          ModerationService moderationService, NotificationService notificationService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.photoRepository = photoRepository;
        this.fileStorageService = fileStorageService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(Long userId) {
        return Mapper.profile(profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found")));
    }

    @Transactional
    public ProfileResponse update(User user, ProfileRequest request) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        moderationService.validateUserContent(request.getBio());
        moderationService.validateUserContent(request.getProfession());
        moderationService.validateUserContent(request.getEducation());
        profile.setBio(request.getBio());
        profile.setEducation(request.getEducation());
        profile.setProfession(request.getProfession());
        profile.setCity(request.getCity());
        if (request.getInterests() != null) {
            profile.getInterests().clear();
            request.getInterests().stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(profile.getInterests()::add);
        }
        Profile saved = profileRepository.save(profile);
        User managedUser = saved.getUser();
        managedUser.setFakeProfileScore(moderationService.fakeProfileScore(saved.getBio(),
                managedUser.getProfilePictureUrl(), saved.getPhotos().size(), saved.getInterests().size()));
        return Mapper.profile(saved);
    }

    @Transactional
    public ProfileResponse uploadProfilePicture(User user, MultipartFile file) {
        String url = fileStorageService.store(file);
        user.setProfilePictureUrl(url);
        userRepository.save(user);
        return get(user.getId());
    }

    @Transactional
    public ProfileResponse uploadPhoto(User user, MultipartFile file) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        Photo photo = new Photo();
        photo.setProfile(profile);
        photo.setUrl(fileStorageService.store(file));
        photo.setPrimaryPhoto(profile.getPhotos().isEmpty());
        photoRepository.save(photo);
        return get(user.getId());
    }

    @Transactional
    public ProfileResponse requestVerification(User user) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        User managedUser = profile.getUser();
        int score = moderationService.fakeProfileScore(profile.getBio(), managedUser.getProfilePictureUrl(),
                profile.getPhotos().size(), profile.getInterests().size());
        managedUser.setFakeProfileScore(score);
        if (score < 50) {
            managedUser.setVerified(true);
            notificationService.create(managedUser, com.loveconnect.app.entity.NotificationType.VERIFICATION,
                    "Your profile verification badge is active");
        } else {
            notificationService.create(managedUser, com.loveconnect.app.entity.NotificationType.MODERATION,
                    "Add a profile photo, bio, and interests before verification");
        }
        return Mapper.profile(profile);
    }
}


