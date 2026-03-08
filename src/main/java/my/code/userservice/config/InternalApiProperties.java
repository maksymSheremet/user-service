package my.code.userservice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "internal.api")
public class InternalApiProperties {

    @NotBlank(message = "Internal API key must not be blank")
    private String key;

    @NotBlank(message = "Internal API path prefix must not be blank")
    private String pathPrefix = "/api/internal/";
}