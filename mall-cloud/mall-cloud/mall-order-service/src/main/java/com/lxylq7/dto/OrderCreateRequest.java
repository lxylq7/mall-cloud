package com.lxylq7.dto;

import lombok.Data;

@Data
public class OrderCreateRequest {
    private Long userId;
    private Long productId;
    private Integer quantity;
}
