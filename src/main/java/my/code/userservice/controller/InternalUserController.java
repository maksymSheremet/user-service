package my.code.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.userservice.dto.response.UserProfileResponse;
import my.code.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long userId) {
        log.info("Internal GET /api/internal/users/{}: request received", userId);

        UserProfileResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserProfileResponse> getUserByEmail(@PathVariable String email) {
        log.info("Internal GET /api/internal/users/by-email/{}: request received", email);

        UserProfileResponse response = userService.getProfileByEmail(email);
        return ResponseEntity.ok(response);
    }
}
