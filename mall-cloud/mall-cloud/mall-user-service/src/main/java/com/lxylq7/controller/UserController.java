package com.lxylq7.controller;

import com.lxylq7.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.lxylq7.dto.UserDTO;

@RestController
public class UserController {

    @GetMapping("/users/{userId}")
    public Result<UserDTO> getById(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return Result.fail("userId不合法");
        }
        UserDTO user = new UserDTO();
        user.setUserId(userId);
        user.setUserName("lxylq");
        user.setStatus("active");
        return Result.ok(user);
    }
}
