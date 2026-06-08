package com.aichat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求限流过滤器
 * 防止 API 被滥用
 */
@Configuration
public class RateLimitConfig {

    // 记录每个 IP 的请求次数
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastResetTimes = new ConcurrentHashMap<>();

    @Bean
    public OncePerRequestFilter rateLimitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                
                String ip = getClientIp(request);
                String path = request.getRequestURI();
                
                // 静态资源不限流
                if (path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".css") || 
                    path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".ico")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                long now = System.currentTimeMillis();
                long windowMs = 60 * 1000; // 1分钟窗口
                int maxRequests = 60; // 每分钟最多60次请求

                // 重置过期的计数
                lastResetTimes.entrySet().removeIf(entry -> now - entry.getValue() > windowMs);
                requestCounts.entrySet().removeIf(entry -> now - lastResetTimes.getOrDefault(entry.getKey(), 0L) > windowMs);

                // 检查限流
                long lastReset = lastResetTimes.computeIfAbsent(ip, k -> now);
                if (now - lastReset > windowMs) {
                    requestCounts.put(ip, new AtomicInteger(0));
                    lastResetTimes.put(ip, now);
                }

                int count = requestCounts.computeIfAbsent(ip, k -> new AtomicInteger(0)).incrementAndGet();
                if (count > maxRequests) {
                    response.setStatus(429);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
                    return;
                }

                filterChain.doFilter(request, response);
            }

            private String getClientIp(HttpServletRequest request) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        };
    }
}