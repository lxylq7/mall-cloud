package com.lxylq7.dto;

import lombok.Data;

@Data
public class UserLoginResponse {
    private String token;
    private String tokenType;
    private long expiresInSeconds; //token过期时间 单位秒
    private UserDTO user;
}
