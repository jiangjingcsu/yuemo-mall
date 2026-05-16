package com.yuemo.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "yuemo.gateway")
public class GatewayProperties {

    /** 白名单路径 — 无需鉴权 */
    private List<String> whiteList = new ArrayList<>();

    /** 半白名单路径 — GET 请求公开访问，格式: "GET:/api/product/**" */
    private List<String> semiWhiteList = new ArrayList<>();

    /** 限流配置 */
    private Map<String, RateLimitConfig> rateLimits = new HashMap<>();

    @Data
    public static class RateLimitConfig {
        /** 匹配路径 */
        private String path;
        /** 每分钟允许请求数 */
        private int permitsPerMinute = 100;
    }
}
