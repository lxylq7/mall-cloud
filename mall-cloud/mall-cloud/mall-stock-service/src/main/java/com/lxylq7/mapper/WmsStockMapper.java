package com.lxylq7.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lxylq7.dto.StockDTO;
import com.lxylq7.dto.StockDeductRequest;
import com.lxylq7.dto.StockDuductResponse;
import com.lxylq7.entity.WmsStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;


@Mapper
public interface WmsStockMapper extends BaseMapper<WmsStock> {

}
