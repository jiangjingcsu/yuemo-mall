package com.yuemo.gateway.filter;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.yuemo.common.core.response.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sentinel 熔断降级 — order=3
 */
@Component
@Order(3)
public class CircuitBreakerFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        String path = request.getRequestURI();
        String resourceName = resolveResource(path);

        if (resourceName == null) {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        try (Entry entry = SphU.entry(resourceName)) {
            filterChain.doFilter(request, response);
        } catch (FlowException e) {
            log.warn("[Sentinel] 限流 resource={} path={}", resourceName, path);
            writeBlocked(response, 429, "请求过于频繁，请稍后再试");
        } catch (DegradeException e) {
            log.warn("[Sentinel] 熔断 resource={} path={}", resourceName, path);
            writeBlocked(response, 503, "服务暂时不可用，请稍后再试");
        } catch (BlockException e) {
            log.warn("[Sentinel] 阻塞 resource={} path={}", resourceName, path);
            writeBlocked(response, 429, "请求被拦截，请稍后再试");
        } catch (Exception e) {
            if (e instanceof java.io.IOException) {
                throw (IOException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private String resolveResource(String path) {
        if (path.startsWith("/api/user"))    return "api-user";
        if (path.startsWith("/api/product")) return "api-product";
        if (path.startsWith("/api/category")) return "api-product";
        if (path.startsWith("/api/order"))   return "api-order";
        if (path.startsWith("/api/payment")) return "api-payment";
        if (path.startsWith("/api/cart"))    return "api-cart";
        if (path.startsWith("/api/coupon"))  return "api-coupon";
        if (path.startsWith("/api/admin"))   return "api-admin";
        return null;
    }

    private void writeBlocked(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(status, message)));
    }
}
