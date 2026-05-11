package com.lxylq7.dto;

import lombok.Data;

@Data
public class StockReleaseRequest {
    private Long productId;
    private Integer quantity;

}
