package com.lxylq7.dto;

import lombok.Data;

@Data
public class StockChangeEvent {
    private String bizNo; //幂等用 uuid
    private String type; //deduct or release
    private Long productId;
    private Integer quantity;
    private Long ts; //时间戳
}
