package com.lxylq7.dto;

import lombok.Data;

@Data
public class StockDTO {
    private Long productId;
    private Integer available;
    private Integer locked;
}
