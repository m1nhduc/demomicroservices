package dmd.prj.authservice.controller;

import dmd.prj.authservice.dto.LoginRequest;
import dmd.prj.authservice.dto.LoginResponse;
import dmd.prj.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validate(@RequestHeader("X-User-Id") String userId,
                                           @RequestHeader("Authorization") String token) {
        String bearerToken = token.replace("Bearer ", "");
        String validatedUserId = authService.validate(userId, bearerToken);
        if (validatedUserId != null) {
            return ResponseEntity.ok(validatedUserId);
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-Id") String userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }
}
