package com.lxylq7.dto;

import lombok.Data;

@Data
public class StockDeductRequest {
    private Long productId;
    private Integer quantity;
}
