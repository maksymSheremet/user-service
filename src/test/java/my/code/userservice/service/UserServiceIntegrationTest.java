package my.code.userservice.service;

import my.code.userservice.BaseIntegrationTest;
import my.code.userservice.dto.event.UserRegisteredEvent;
import my.code.userservice.dto.request.CompleteOnboardingRequest;
import my.code.userservice.dto.request.UpdateProfileRequest;
import my.code.userservice.dto.response.UserProfileResponse;
import my.code.userservice.entity.UserStatus;
import my.code.userservice.exception.UserNotFoundException;
import my.code.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserService Integration")
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "integration@example.com";

    @AfterEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("createProfileFromEvent")
    class CreateProfileFromEvent {

        @Test
        @DisplayName("should persist user to database with correct fields")
        void shouldPersistUserToDatabase() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    USER_ID, EMAIL, "John Doe", null,
                    "Europe/Kyiv", "uk", Instant.now()
            );

            userService.createProfileFromEvent(event);

            var saved = userRepository.findById(USER_ID);
            assertThat(saved).isPresent();
            assertThat(saved.get().getEmail()).isEqualTo(EMAIL);
            assertThat(saved.get().getFullName()).isEqualTo("John Doe");
            assertThat(saved.get().getTimezone()).isEqualTo("Europe/Kyiv");
            assertThat(saved.get().getLanguage()).isEqualTo("uk");

            assertThat(saved.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(saved.get().isOnboardingCompleted()).isFalse();
            assertThat(saved.get().getCreatedAt()).isNotNull();
            assertThat(saved.get().getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("should be idempotent on duplicate event")
        void shouldBeIdempotentOnDuplicateEvent() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    USER_ID, EMAIL, "John Doe", null,
                    "UTC", "en", Instant.now()
            );

            userService.createProfileFromEvent(event);
            userService.createProfileFromEvent(event);

            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should use mapper defaults when event timezone/language are null")
        void shouldUseDefaultsWhenEventFieldsAreNull() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    USER_ID, EMAIL, null, null,
                    null, null, Instant.now()
            );

            userService.createProfileFromEvent(event);

            var saved = userRepository.findById(USER_ID);
            assertThat(saved).isPresent();
            assertThat(saved.get().getTimezone()).isEqualTo("UTC");
            assertThat(saved.get().getLanguage()).isEqualTo("en");
            assertThat(saved.get().getFullName()).isNull();
            assertThat(saved.get().getAvatarUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("should return correct response from database")
        void shouldReturnCorrectResponse() {
            userService.createProfileFromEvent(new UserRegisteredEvent(
                    USER_ID, EMAIL, "John Doe", null, "UTC", "en", Instant.now()
            ));

            UserProfileResponse response = userService.getProfile(USER_ID);

            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.fullName()).isEqualTo("John Doe");
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(response.onboardingCompleted()).isFalse();
        }

        @Test
        @DisplayName("should throw UserNotFoundException for non-existent user")
        void shouldThrowForNonExistentUser() {
            assertThatThrownBy(() -> userService.getProfile(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getProfileByEmail")
    class GetProfileByEmail {

        @Test
        @DisplayName("should return profile by email")
        void shouldReturnProfileByEmail() {
            userService.createProfileFromEvent(new UserRegisteredEvent(
                    USER_ID, EMAIL, "John Doe", null, "UTC", "en", Instant.now()
            ));

            UserProfileResponse response = userService.getProfileByEmail(EMAIL);

            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.id()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw UserNotFoundException for non-existent email")
        void shouldThrowForNonExistentEmail() {
            assertThatThrownBy(() -> userService.getProfileByEmail("nonexistent@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should persist partial update — only non-null fields change")
        void shouldPersistPartialUpdate() {
            userService.createProfileFromEvent(new UserRegisteredEvent(
                    USER_ID, EMAIL, "John Doe", null, "UTC", "en", Instant.now()
            ));

            userService.updateProfile(USER_ID,
                    new UpdateProfileRequest(null, null, "Europe/Kyiv", null));

            var updated = userRepository.findById(USER_ID).orElseThrow();
            assertThat(updated.getTimezone()).isEqualTo("Europe/Kyiv");
            assertThat(updated.getFullName()).isEqualTo("John Doe");
            assertThat(updated.getLanguage()).isEqualTo("en");
            assertThat(updated.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("completeOnboarding")
    class CompleteOnboarding {

        @Test
        @DisplayName("should set onboardingCompleted=true and persist to database")
        void shouldCompleteOnboardingInDatabase() {
            userService.createProfileFromEvent(new UserRegisteredEvent(
                    USER_ID, EMAIL, null, null, "UTC", "en", Instant.now()
            ));

            userService.completeOnboarding(USER_ID,
                    new CompleteOnboardingRequest("John Doe", "Europe/Kyiv", "uk"));

            var updated = userRepository.findById(USER_ID).orElseThrow();
            assertThat(updated.isOnboardingCompleted()).isTrue();
            assertThat(updated.getTimezone()).isEqualTo("Europe/Kyiv");
            assertThat(updated.getLanguage()).isEqualTo("uk");
            assertThat(updated.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should be idempotent — second call does not overwrite data")
        void shouldBeIdempotentOnSecondCall() {
            userService.createProfileFromEvent(new UserRegisteredEvent(
                    USER_ID, EMAIL, null, null, "UTC", "en", Instant.now()));
            userService.completeOnboarding(USER_ID,
                    new CompleteOnboardingRequest("John Doe", "Europe/Kyiv", "uk"));
            userService.completeOnboarding(USER_ID,
                    new CompleteOnboardingRequest("Other Name", "America/New_York", "en"));

            var updated = userRepository.findById(USER_ID).orElseThrow();

            assertThat(updated.getFullName()).isEqualTo("John Doe");
            assertThat(updated.getTimezone()).isEqualTo("Europe/Kyiv");
        }
    }
}