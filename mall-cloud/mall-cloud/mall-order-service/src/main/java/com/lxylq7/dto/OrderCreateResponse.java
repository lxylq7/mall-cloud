package com.lxylq7.dto;

import lombok.Data;

@Data
public class OrderCreateResponse {
    private Boolean success; //订单创建是否成功
    private String message; //描述信息
    private String orderNo; //订单号
    private Long userId; //用户id
    private ProductDTO product; //购买的商品信息
    private Integer quantity; //购买的数量
}
