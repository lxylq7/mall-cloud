package com.lxylq7.client;

import com.lxylq7.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mall-user-service")
public interface UserClient {

    @GetMapping("/users/{id}")
    UserDTO getById(@PathVariable("id") Long id);
}
