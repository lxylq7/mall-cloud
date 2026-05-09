package com.lxylq7.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;  //无精度丢失
    private Integer stock;
}
