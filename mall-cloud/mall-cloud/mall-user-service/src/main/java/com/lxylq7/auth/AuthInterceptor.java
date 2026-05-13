package com.lxylq7.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxylq7.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String auth = request.getHeader("Authorization");
        String token = extractBearer(auth);
        JwtUtil.JwtClaims claims = jwtUtil.parseAndValidate(token);
        if (claims == null) {
            response.setStatus(401);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "未登录或token无效")));
            return false;
        }
        //token有效 设置当前用户上下文
        CurrentUserContext.CurrentUser user = new CurrentUserContext.CurrentUser();
        user.setUserId(claims.getUserId());
        user.setUsername(claims.getUsername());
        CurrentUserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private static String extractBearer(String auth) {
        if (auth == null || auth.isBlank()) {
            return null;
        }
        if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return auth.trim();
    }
}
