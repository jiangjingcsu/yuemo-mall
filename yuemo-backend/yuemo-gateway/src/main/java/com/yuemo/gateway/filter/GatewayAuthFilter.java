package com.yuemo.gateway.filter;

import com.yuemo.common.core.response.Result;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.security.constant.AuthRedisKeyConstants;
import com.yuemo.common.security.utils.JwtTokenProvider;
import com.yuemo.gateway.properties.GatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 统一鉴权 — order=1 最高优先级
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthFilter.class);
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 白名单 — 直接放行
        if (matchesWhiteList(path, method)) {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        try {
            String token = extractToken(request);
            if (!StringUtils.hasText(token)) {
                writeUnauthorized(response, ResultCode.UNAUTHORIZED, "请先登录");
                return;
            }

            if (!jwtTokenProvider.validateToken(token)) {
                writeUnauthorized(response, ResultCode.USER_TOKEN_EXPIRED, "Token 已过期，请重新登录");
                return;
            }

            String blacklistKey = AuthRedisKeyConstants.TOKEN_BLACKLIST_PREFIX + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                writeUnauthorized(response, ResultCode.UNAUTHORIZED, "Token 已失效，请重新登录");
                return;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);

            // admin 接口校验角色
            if (path.startsWith("/api/admin/")) {
                String roleKey = AuthRedisKeyConstants.USER_ROLE_PREFIX + userId;
                String role = (String) redisTemplate.opsForValue().get(roleKey);
                if (!"ADMIN".equals(role)) {
                    writeForbidden(response, "无访问权限，需要管理员角色");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            if (!(e instanceof java.io.IOException)) {
                log.error("[GatewayAuth] 鉴权异常", e);
                writeUnauthorized(response, ResultCode.INTERNAL_ERROR, "鉴权服务异常");
                return;
            }
            throw (IOException) e;
        }
    }

    private boolean matchesWhiteList(String path, String method) {
        for (String pattern : gatewayProperties.getWhiteList()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        if (HttpMethod.GET.matches(method)) {
            for (String semi : gatewayProperties.getSemiWhiteList()) {
                // 格式: "GET:/api/product/**"
                int colonIdx = semi.indexOf(':');
                if (colonIdx > 0) {
                    String semiMethod = semi.substring(0, colonIdx);
                    String semiPath = semi.substring(colonIdx + 1);
                    if (method.equalsIgnoreCase(semiMethod) && pathMatcher.match(semiPath, path)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, ResultCode code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(code.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(ResultCode.FORBIDDEN.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
