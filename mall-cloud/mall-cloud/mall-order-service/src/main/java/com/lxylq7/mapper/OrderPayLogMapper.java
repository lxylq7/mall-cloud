package com.lxylq7.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderPayLogMapper {

    @Insert("""
        INSERT IGNORE INTO oms_order_pay_log (pay_no, order_no)
        VALUES (#{payNo}, #{orderNo})
    """)
    int insertIgnore(@Param("payNo") String payNo, @Param("orderNo") String orderNo);

    @Select("""
        SELECT pay_no
        FROM oms_order_pay_log
        WHERE order_no = #{orderNo}
        LIMIT 1
    """)
    String selectPayNoByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
    SELECT order_no
    FROM oms_order_pay_log
    WHERE pay_no = #{payNo}
    LIMIT 1
    """)
    String selectOrderNoByPayNo(@Param("payNo") String payNo);
}
