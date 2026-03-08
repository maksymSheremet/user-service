package my.code.userservice.service.impl;

import my.code.userservice.dto.event.UserRegisteredEvent;
import my.code.userservice.dto.request.CompleteOnboardingRequest;
import my.code.userservice.dto.request.UpdateProfileRequest;
import my.code.userservice.dto.response.UserProfileResponse;
import my.code.userservice.entity.User;
import my.code.userservice.entity.UserStatus;
import my.code.userservice.exception.UserNotFoundException;
import my.code.userservice.mapper.UserMapper;
import my.code.userservice.mapper.UserMapperImpl;
import my.code.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Spy
    private UserMapper userMapper = new UserMapperImpl();

    @InjectMocks
    private UserServiceImpl userService;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@example.com";

    private User testUser;
    private UserProfileResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .fullName("John Doe")
                .timezone("UTC")
                .language("en")
                .onboardingCompleted(false)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        testResponse = new UserProfileResponse(
                USER_ID, EMAIL, "John Doe", null,
                "UTC", "en", false, UserStatus.ACTIVE, Instant.now()
        );
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("should return profile when user exists")
        void shouldReturnProfileWhenUserExists() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testResponse);

            UserProfileResponse result = userService.getProfile(USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo(EMAIL);

            verify(userRepository).findById(USER_ID);
            verify(userMapper).toResponse(testUser);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(USER_ID))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(String.valueOf(USER_ID));

            verify(userMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("getProfileByEmail")
    class GetProfileByEmail {

        @Test
        @DisplayName("should return profile when email exists")
        void shouldReturnProfileWhenEmailExists() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testResponse);

            UserProfileResponse result = userService.getProfileByEmail(EMAIL);

            assertThat(result.email()).isEqualTo(EMAIL);

            verify(userRepository).findByEmail(EMAIL);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when email not found")
        void shouldThrowWhenEmailNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfileByEmail(EMAIL))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(EMAIL);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should update only non-null fields (partial update)")
        void shouldUpdateOnlyNonNullFields() {
            UpdateProfileRequest request = new UpdateProfileRequest(
                    null, null, "Europe/Kyiv", null
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(testResponse);

            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getTimezone()).isEqualTo("Europe/Kyiv");
            assertThat(testUser.getFullName()).isEqualTo("John Doe");
            assertThat(testUser.getLanguage()).isEqualTo("en");

            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should update all fields when all non-null")
        void shouldUpdateAllFieldsWhenAllNonNull() {
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "Jane Doe", "https://avatar.url", "America/New_York", "uk"
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(testResponse);

            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getFullName()).isEqualTo("Jane Doe");
            assertThat(testUser.getAvatarUrl()).isEqualTo("https://avatar.url");
            assertThat(testUser.getTimezone()).isEqualTo("America/New_York");
            assertThat(testUser.getLanguage()).isEqualTo("uk");
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            UpdateProfileRequest updateProfileRequest =
                    new UpdateProfileRequest(null, null, null, null);

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, updateProfileRequest))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("completeOnboarding")
    class CompleteOnboarding {

        @Test
        @DisplayName("should complete onboarding and set fields")
        void shouldCompleteOnboardingAndSetFields() {
            CompleteOnboardingRequest request =
                    new CompleteOnboardingRequest("John Doe", "Europe/Kyiv", "uk");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(testResponse);

            userService.completeOnboarding(USER_ID, request);

            assertThat(testUser.isOnboardingCompleted()).isTrue();
            assertThat(testUser.getTimezone()).isEqualTo("Europe/Kyiv");
            assertThat(testUser.getLanguage()).isEqualTo("uk");
            assertThat(testUser.getFullName()).isEqualTo("John Doe");

            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should be idempotent when onboarding already completed")
        void shouldBeIdempotentWhenAlreadyCompleted() {
            testUser.setOnboardingCompleted(true);
            testUser.setTimezone("America/New_York");

            CompleteOnboardingRequest request =
                    new CompleteOnboardingRequest(null, "Europe/Kyiv", "uk");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testResponse);

            userService.completeOnboarding(USER_ID, request);

            assertThat(testUser.getTimezone()).isEqualTo("America/New_York");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should complete onboarding without fullName when fullName is null")
        void shouldCompleteOnboardingWithoutFullName() {
            CompleteOnboardingRequest request =
                    new CompleteOnboardingRequest(null, "UTC", "en");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(testResponse);

            userService.completeOnboarding(USER_ID, request);

            assertThat(testUser.isOnboardingCompleted()).isTrue();
            assertThat(testUser.getFullName()).isEqualTo("John Doe");
        }
    }

    @Nested
    @DisplayName("createProfileFromEvent")
    class CreateProfileFromEvent {

        private UserRegisteredEvent event;

        @BeforeEach
        void setUp() {
            event = new UserRegisteredEvent(
                    USER_ID, EMAIL, "John Doe", null,
                    "UTC", "en", Instant.now()
            );
        }

        @Test
        @DisplayName("should create profile when not exists")
        void shouldCreateProfileWhenNotExists() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);
            when(userMapper.fromEvent(event)).thenReturn(testUser);
            when(userRepository.save(testUser)).thenReturn(testUser);

            userService.createProfileFromEvent(event);

            verify(userMapper).fromEvent(event);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should skip when profile already exists (idempotency)")
        void shouldSkipWhenProfileAlreadyExists() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);

            userService.createProfileFromEvent(event);

            verify(userMapper, never()).fromEvent(any());
            verify(userRepository, never()).save(any());
        }
    }
}