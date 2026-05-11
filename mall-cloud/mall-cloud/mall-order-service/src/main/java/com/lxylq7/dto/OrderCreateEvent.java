package com.lxylq7.dto;

import lombok.Data;

@Data
public class OrderCreateEvent {
    private String bizNo;      // 幂等键 uuid
    private String orderNo;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Long ts;
}