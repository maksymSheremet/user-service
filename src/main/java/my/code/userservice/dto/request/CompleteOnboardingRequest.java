package my.code.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteOnboardingRequest(

        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Timezone is required to complete onboarding")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9_/+\\-]*$",
                message = "Invalid timezone. Use IANA format (e.g. Europe/Kyiv, UTC)"
        )
        String timezone,

        @NotBlank(message = "Language is required to complete onboarding")
        @Pattern(
                regexp = "^[a-z]{2}$",
                message = "Language must be a 2-letter ISO 639-1 code (e.g. en, uk)"
        )
        String language

) {
}
