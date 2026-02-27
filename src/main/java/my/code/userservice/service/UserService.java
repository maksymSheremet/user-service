package my.code.userservice.service;

import my.code.userservice.dto.event.UserRegisteredEvent;
import my.code.userservice.dto.request.CompleteOnboardingRequest;
import my.code.userservice.dto.request.UpdateProfileRequest;
import my.code.userservice.dto.response.UserProfileResponse;

public interface UserService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse getProfileByEmail(String email);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserProfileResponse completeOnboarding(Long userId, CompleteOnboardingRequest request);

    void createProfileFromEvent(UserRegisteredEvent event);
}

