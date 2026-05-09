package com.lxylq7.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.lxylq7.entity.OmsOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OmsOrderMapper extends BaseMapper<OmsOrder> {

}
