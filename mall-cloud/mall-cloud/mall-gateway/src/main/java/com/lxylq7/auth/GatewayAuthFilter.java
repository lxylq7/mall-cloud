package com.lxylq7.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxylq7.common.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * 实现了过滤器 所有请求都要经过
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;
    private final javax.crypto.SecretKey key;

    public GatewayAuthFilter(
            ObjectMapper objectMapper,
            @Value("${user.jwt.secret:change-me}") String secret
    ) {
        this.objectMapper = objectMapper;
        this.key = Keys.hmacShaKeyFor(normalizeSecret(secret));
    }

    /**
     * 认证过滤器的顺序，-100 表示在其他过滤器之前执行
     * @return
     */
    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        //公共路径 直接放行
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = extractBearer(auth);
        Claims claims = parseClaims(token);
        if (claims == null) {
            return writeJson(exchange, 401, Result.fail(401, "未登录或token无效"));
        }

        Object uidObj = claims.get("uid");
        String username = claims.get("username", String.class);
        if (!(uidObj instanceof Number) || username == null) {
            return writeJson(exchange, 401, Result.fail(401, "未登录或token无效"));
        }

        long uid = ((Number) uidObj).longValue();

        //把用户信息添加到请求头中 传给下游微服务
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(uid))
                .header("X-Username", username)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublicPath(String path) {
        if (path == null) return true;
        return path.equals("/user/login")
                || path.equals("/user/register")
                || path.startsWith("/user/users/")
                || path.startsWith("/actuator/");
    }

    private Claims parseClaims(String token) {
        try {
            if (token == null || token.isBlank()) return null;
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration() == null) return null;
            long expSec = claims.getExpiration().toInstant().getEpochSecond();
            long nowSec = Instant.now().getEpochSecond();
            if (expSec <= nowSec) return null;

            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractBearer(String auth) {
        if (auth == null || auth.isBlank()) return null;
        if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return auth.trim();
    }

    private static byte[] normalizeSecret(String secret) {
        if (secret == null) secret = "";
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) return raw;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException("secret处理失败");
        }
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, int status, Object body) {
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"success\":false,\"code\":401,\"message\":\"未登录或token无效\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatusCode.valueOf(status));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}