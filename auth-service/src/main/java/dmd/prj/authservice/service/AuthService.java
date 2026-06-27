package dmd.prj.authservice.service;

import dmd.prj.authservice.domain.User;
import dmd.prj.authservice.dto.LoginRequest;
import dmd.prj.authservice.dto.LoginResponse;
import dmd.prj.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final long ACCESS_TOKEN_TTL = 900;  // 15 phút
    private static final long REFRESH_TOKEN_TTL = 604800;  // 7 ngày

    public LoginResponse login(LoginRequest request) {
        String userId = request.getUserId();
        
        userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User();
            newUser.setUserId(userId);
            return userRepository.save(newUser);
        });

        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set("access_token:" + userId, accessToken, ACCESS_TOKEN_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("refresh_token:" + userId, refreshToken, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);

        log.info("User {} logged in", userId);
        return new LoginResponse(accessToken, refreshToken);
    }

    public String validate(String userId, String token) {
        String storedToken = redisTemplate.opsForValue().get("access_token:" + userId);
        if (token.equals(storedToken)) {
            return userId;
        }
        return null;
    }

    public void logout(String userId) {
        redisTemplate.delete("access_token:" + userId);
        redisTemplate.delete("refresh_token:" + userId);
        log.info("User {} logged out", userId);
    }
}
