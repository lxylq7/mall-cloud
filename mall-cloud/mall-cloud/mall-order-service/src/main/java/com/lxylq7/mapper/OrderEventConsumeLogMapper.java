package com.lxylq7.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface OrderEventConsumeLogMapper {

    @Insert("""
        INSERT IGNORE INTO oms_order_event_consume_log (biz_no, event_type, consume_status)
        VALUES (#{bizNo}, #{eventType}, 'PROCESSING')
    """)
    int insertIgnore(@Param("bizNo") String bizNo, @Param("eventType") String eventType);

    @Select("""
        SELECT consume_status
        FROM oms_order_event_consume_log
        WHERE biz_no = #{bizNo} AND event_type = #{eventType}
        LIMIT 1
    """)
    String selectStatus(@Param("bizNo") String bizNo, @Param("eventType") String eventType);

    @Update("""
        UPDATE oms_order_event_consume_log
        SET consume_status = 'DONE'
        WHERE biz_no = #{bizNo} AND event_type = #{eventType}
    """)
    int markDone(@Param("bizNo") String bizNo, @Param("eventType") String eventType);
}