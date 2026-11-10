package com.shangxu.crm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录态缓存（Redis），实现退出登录即失效、权限实时刷新
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String LOGIN_KEY = "crm:login:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void save(LoginUser loginUser) {
        redisTemplate.opsForValue().set(LOGIN_KEY + loginUser.getId(), loginUser,
                jwtTokenProvider.getExpire(), TimeUnit.SECONDS);
    }

    public LoginUser get(Long userId) {
        Object value = redisTemplate.opsForValue().get(LOGIN_KEY + userId);
        return value instanceof LoginUser loginUser ? loginUser : null;
    }

    public void remove(Long userId) {
        redisTemplate.delete(LOGIN_KEY + userId);
    }

    public void refresh(LoginUser loginUser) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LOGIN_KEY + loginUser.getId()))) {
            save(loginUser);
        }
    }
}
