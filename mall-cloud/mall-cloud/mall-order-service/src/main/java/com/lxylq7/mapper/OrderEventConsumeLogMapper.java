package com.lxylq7.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderEventConsumeLogMapper {

    @Insert("""
        INSERT IGNORE INTO oms_order_event_consume_log (biz_no, event_type)
        VALUES (#{bizNo}, #{eventType})
    """)
    int insertIgnore(@Param("bizNo") String bizNo, @Param("eventType") String eventType);
}