package com.lxylq7.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lxylq7.auth.JwtUtil;
import com.lxylq7.dto.UserDTO;
import com.lxylq7.dto.UserLoginResponse;
import com.lxylq7.dto.UserRegisterRequest;
import com.lxylq7.entity.UmsUser;
import com.lxylq7.mapper.UmsUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UmsUserMapper umsUserMapper;

    public UserDTO register(UserRegisterRequest req) {
        String username = normalizeUsername(req.getUsername());  //规范格式
        UmsUser exists = umsUserMapper.selectOne(
                new LambdaQueryWrapper<UmsUser>()
                        .eq(UmsUser::getUsername, username)
                        .last("limit 1")
        );
        if (exists != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String passwordHash = hashPassword(req.getPassword());

        UmsUser u = new UmsUser();
        u.setUsername(username);
        u.setPasswordHash(passwordHash);
        u.setStatus("active");
        u.setCreatedAt(LocalDateTime.now());

        try {
            umsUserMapper.insert(u);
        } catch (Exception e) {
            throw new IllegalArgumentException("注册失败");
        }
        return toUserDTO(u);
    }

    public UserLoginResponse login(String usernameRaw, String passwordRaw) {
        String username = normalizeUsername(usernameRaw);
        UmsUser u = umsUserMapper.selectOne(
                new LambdaQueryWrapper<UmsUser>()
                        .eq(UmsUser::getUsername, username)
                        .last("limit 1")
        );
        if (u == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!"active".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("用户不可用");
        }
        if (!verifyPassword(passwordRaw, u.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = jwtUtil.createToken(u.getId(), u.getUsername());
        UserLoginResponse resp = new UserLoginResponse();
        resp.setToken(token);
        resp.setTokenType("Bearer");
        resp.setExpiresInSeconds(jwtUtil.getTtlSeconds());
        resp.setUser(toUserDTO(u));
        return resp;
    }

    public UserDTO getById(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        UmsUser u = umsUserMapper.selectById(userId);
        if (u == null) {
            return null;
        }
        return toUserDTO(u);
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        return username.trim().toLowerCase();
    }

    private static UserDTO toUserDTO(UmsUser u) {
        UserDTO dto = new UserDTO();
        dto.setUserId(u.getId());
        dto.setUserName(u.getUsername());
        dto.setStatus(u.getStatus());
        return dto;
    }

    private static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password不能为空");
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = sha256(concat(salt, password.getBytes(StandardCharsets.UTF_8)));
        return base64Url(salt) + "." + base64Url(hash);
    }

    private static boolean verifyPassword(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        int idx = stored.indexOf('.');
        if (idx <= 0 || idx >= stored.length() - 1) {
            return false;
        }
        byte[] salt = base64UrlDecode(stored.substring(0, idx));
        byte[] expected = base64UrlDecode(stored.substring(idx + 1));
        byte[] actual = sha256(concat(salt, password.getBytes(StandardCharsets.UTF_8)));
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("hash失败");
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

}
