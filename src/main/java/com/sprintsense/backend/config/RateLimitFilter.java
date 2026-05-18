package com.sprintsense.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Naive in-memory per-IP rate limiter for /api/ai/* endpoints. Resets the
 * counter every 60 seconds. Suitable for hackathon scale and demos where the
 * goal is to avoid accidentally burning the GitHub Models quota on a busy
 * page or a buggy frontend loop. Not for production multi-instance use --
 * counters are per-process and the bucket map grows unbounded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final String AI_PATH_PREFIX = "/api/ai/";

    private final int requestsPerMinute;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${sprintsense.ratelimit.requests-per-minute:30}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!req.getRequestURI().startsWith(AI_PATH_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }
        Bucket b = buckets.computeIfAbsent(clientIp(req), k -> new Bucket());
        long now = System.currentTimeMillis();
        int current;
        synchronized (b) {
            if (now - b.windowStart > WINDOW_MS) {
                b.windowStart = now;
                b.count.set(0);
            }
            current = b.count.incrementAndGet();
        }
        if (current > requestsPerMinute) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.setHeader("Retry-After", "60");
            res.getWriter().write(
                    "{\"code\":\"rate_limited\",\"message\":\"too many requests; try again in a minute\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static final class Bucket {
        final AtomicInteger count = new AtomicInteger();
        long windowStart = System.currentTimeMillis();
    }
}
