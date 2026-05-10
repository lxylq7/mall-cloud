package com.lxylq7.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_stock")
public class WmsStock {
    @TableId
    private Long id;
    private Long productId;
    private Integer available;
    private Integer locked;
}
