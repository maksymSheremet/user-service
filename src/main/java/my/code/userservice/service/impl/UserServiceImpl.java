package my.code.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.userservice.dto.event.UserRegisteredEvent;
import my.code.userservice.dto.request.CompleteOnboardingRequest;
import my.code.userservice.dto.request.UpdateProfileRequest;
import my.code.userservice.dto.response.UserProfileResponse;
import my.code.userservice.entity.User;
import my.code.userservice.exception.UserNotFoundException;
import my.code.userservice.mapper.UserMapper;
import my.code.userservice.repository.UserRepository;
import my.code.userservice.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserProfileResponse getProfile(Long userId) {
        return userMapper.toResponse(findById(userId));
    }

    @Override
    public UserProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);
        userMapper.updateFromRequest(request, user);
        User saved = userRepository.save(user);
        log.debug("Profile updated: userId={}", userId);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse completeOnboarding(Long userId, CompleteOnboardingRequest request) {
        User user = findById(userId);

        if (user.isOnboardingCompleted()) {
            log.debug("Onboarding already completed for userId={}, skipping", userId);
            return userMapper.toResponse(user);
        }

        user.setTimezone(request.timezone());
        user.setLanguage(request.language());
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        user.setOnboardingCompleted(true);

        User saved = userRepository.save(user);
        log.info("Onboarding completed: userId={}", userId);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void createProfileFromEvent(UserRegisteredEvent event) {
        if (userRepository.existsById(event.userId())) {
            log.warn("Profile already exists for userId={}, skipping duplicate event", event.userId());
            return;
        }

        User user = userMapper.fromEvent(event);
        userRepository.save(user);
        log.info("Profile created from Kafka event: userId={}, email={}", event.userId(), event.email());
    }

    private User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
