package com.lxylq7.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {
    @NotBlank(message = "username不能为空")
    @Size(min = 3, max = 32, message = "username长度需在3~32之间")
    private String username;

    @NotBlank(message = "password不能为空")
    @Size(min = 6, max = 64, message = "password长度需在6~64之间")
    private String password;
}
