package my.code.userservice.dto.response;

import my.code.userservice.entity.UserStatus;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String timezone,
        String language,
        boolean onboardingCompleted,
        UserStatus status,
        Instant createdAt
) {
}
