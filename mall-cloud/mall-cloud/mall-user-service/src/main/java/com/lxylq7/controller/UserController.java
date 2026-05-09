package com.lxylq7.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.lxylq7.dto.UserDTO;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/users/{userId}")
    public UserDTO getById(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        UserDTO user = new UserDTO();
        user.setUserId(userId);
        user.setUserName("lxylq");
        user.setStatus("active");
        return user;
    }
}
