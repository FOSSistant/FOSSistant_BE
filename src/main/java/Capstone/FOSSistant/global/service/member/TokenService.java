package Capstone.FOSSistant.global.service.member;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(Long memberId, String refreshToken) {
        redisTemplate.opsForValue()
                .set("refresh:" + memberId, refreshToken, Duration.ofDays(7));
    }

    public boolean isRefreshTokenValid(Long memberId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get("refresh:" + memberId);
        return stored != null && stored.equals(refreshToken);
    }

    public void deleteRefreshToken(Long memberId) {
        redisTemplate.delete("refresh:" + memberId);
    }
}