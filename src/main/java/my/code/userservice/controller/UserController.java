package my.code.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.userservice.dto.request.CompleteOnboardingRequest;
import my.code.userservice.dto.request.UpdateProfileRequest;
import my.code.userservice.dto.response.UserProfileResponse;
import my.code.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("GET /api/me: userId={}", userId);

        UserProfileResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateMyProfile(Authentication authentication,
                                                               @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = extractUserId(authentication);
        log.debug("PUT /api/me: userId={}", userId);

        UserProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/onboarding")
    public ResponseEntity<UserProfileResponse> completeOnboarding(Authentication authentication,
                                                                  @Valid @RequestBody CompleteOnboardingRequest request) {
        Long userId = extractUserId(authentication);
        log.debug("POST /api/me/onboarding: userId={}", userId);

        UserProfileResponse response = userService.completeOnboarding(userId, request);
        return ResponseEntity.ok(response);
    }

    private Long extractUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}