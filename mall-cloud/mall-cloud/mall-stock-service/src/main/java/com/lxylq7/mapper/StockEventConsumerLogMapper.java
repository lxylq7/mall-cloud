package com.lxylq7.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockEventConsumerLogMapper {

    @Insert("""
        INSERT IGNORE INTO stock_event_consume_log (biz_no, event_type)
        VALUES (#{bizNo}, #{eventType})
    """)
    int insertIgnore(@Param("bizNo") String bizNo, @Param("eventType") String eventType);
}
