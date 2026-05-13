package com.lxylq7.dto;

import lombok.Data;

@Data
public class OrderPayEvent {

    private String payNo;
    private String orderNo;
    private Long ts;
}
