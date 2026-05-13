package com.lxylq7.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockDeductRequest {
    @NotNull(message = "productId不能为空")
    private Long productId;
    @NotNull(message = "quantity不能为空")
    @Min(value = 1, message = "quantity最小为1")
    private Integer quantity;
}
