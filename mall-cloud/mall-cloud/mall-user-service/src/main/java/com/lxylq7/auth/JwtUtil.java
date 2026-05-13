package com.lxylq7.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret;
    private final long ttlSeconds;
    private final javax.crypto.SecretKey key;

    public JwtUtil(
            @Value("${user.jwt.secret:change-me}") String secret,
            @Value("${user.jwt.ttl-seconds:7200}") long ttlSeconds
    ) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds <= 0 ? 7200 : ttlSeconds;
        this.key = Keys.hmacShaKeyFor(normalizeSecret(secret));
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public String createToken(long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .claim("uid", userId)
                .claim("username", username == null ? "" : username)
                .issuedAt(Date.from(now)) //设置签发时间
                .expiration(Date.from(exp)) //设置过期时间
                .signWith(key) //加密签名
                .compact(); //生成JWT字符串
    }

    public JwtClaims parseAndValidate(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }
            Claims claims = Jwts.parser()
                    .verifyWith(key)  // 用密钥验证签名
                    .build()  //构造解析器
                    .parseSignedClaims(token) //解析JWT字符串
                    .getPayload(); //获取用户数据

            Object uidObj = claims.get("uid");
            String username = claims.get("username", String.class);
            Date exp = claims.getExpiration();
            if (uidObj == null || username == null || exp == null) {
                return null;
            }

            long uid;
            if (uidObj instanceof Number n) { //判断是不是数字类型
                uid = n.longValue();
            } else {
                return null;
            }

            long expSec = exp.toInstant().getEpochSecond();
            long nowSec = Instant.now().getEpochSecond();
            if (expSec <= nowSec) {
                //已经过期
                return null;
            }

            JwtClaims c = new JwtClaims();
            c.userId = uid;
            c.username = username;
            c.exp = expSec;
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] normalizeSecret(String secret) {
        if (secret == null) {
            secret = "";
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException("secret处理失败");
        }
    }

    public static class JwtClaims {
        private long userId;
        private String username;
        private long exp;

        public long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public long getExp() {
            return exp;
        }
    }
}
