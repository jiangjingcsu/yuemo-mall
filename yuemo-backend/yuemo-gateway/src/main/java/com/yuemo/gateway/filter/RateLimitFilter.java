package com.yuemo.gateway.filter;

import com.yuemo.common.core.response.Result;
import com.yuemo.gateway.properties.GatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Redis 滑动窗口限流 — order=2
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final StringRedisTemplate redisTemplate;
    private final GatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) or 60 " +
            "local current = redis.call('INCR', key) " +
            "if current == 1 then redis.call('EXPIRE', key, window) end " +
            "return current";

    public RateLimitFilter(StringRedisTemplate redisTemplate,
                           GatewayProperties gatewayProperties) {
        this.redisTemplate = redisTemplate;
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String path = request.getRequestURI();

        int permits = getPermits(path);
        if (permits <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildRateLimitKey(request, path);
        try {
            Long currentCount = redisTemplate.execute(
                    (RedisScript<Long>) RedisScript.of(RATE_LIMIT_SCRIPT, Long.class),
                    Collections.singletonList(key),
                    String.valueOf(permits),
                    "60"
            );

            if (currentCount != null && currentCount > permits) {
                log.warn("[RateLimit] 限流触发 path={} key={} count={}", path, key, currentCount);
                writeRateLimited(response);
                return;
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof ServletException) {
                throw (ServletException) e;
            }
            log.warn("[RateLimit] Redis 不可用，降级放行 path={}", path, e);
            filterChain.doFilter(request, response);
        }
    }

    private int getPermits(String path) {
        for (var entry : gatewayProperties.getRateLimits().entrySet()) {
            var config = entry.getValue();
            if (config.getPath() != null && path.startsWith(config.getPath())) {
                return config.getPermitsPerMinute();
            }
        }
        return 0;
    }

    private String buildRateLimitKey(HttpServletRequest request, String path) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            return "rate:" + path + ":" + userId;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return "rate:" + path + ":" + ip;
    }

    private void writeRateLimited(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(429, "请求过于频繁，请稍后再试")));
    }
}
