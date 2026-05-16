package com.yuemo.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

/**
 * 统一请求日志 — traceId + 请求/响应记录
 */
@Component
@Order(Integer.MAX_VALUE - 100)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put("traceId", traceId);
        response.setHeader(TRACE_HEADER, traceId);

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query != null ? path + "?" + query : path;

        ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception e) {
            log.error("[{}] {} {} {}ms ERROR",
                    traceId, method, fullPath, System.currentTimeMillis() - start, e);
            throw new RuntimeException(e);
        } finally {
            long cost = System.currentTimeMillis() - start;
            int status = responseWrapper.getStatus();

            String logLine = String.format("[%s] %s %s %d %dms",
                    traceId, method, fullPath, status, cost);

            if (status >= 500) {
                log.error(logLine);
            } else if (status >= 400) {
                log.warn(logLine);
            } else {
                log.info(logLine);
            }

            responseWrapper.copyBodyToResponse();
            MDC.clear();
        }
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }
        return new ContentCachingRequestWrapper(request);
    }
}
