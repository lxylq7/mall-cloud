package com.lxylq7.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.dto.OrderPayEvent;
import com.lxylq7.entity.OmsOrder;
import com.lxylq7.mapper.OmsOrderMapper;
import com.lxylq7.mapper.OrderEventConsumeLogMapper;
import com.lxylq7.mapper.OrderPayLogMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OrderPayConsumer {

    @Bean
    public Consumer<OrderPayEvent> orderPayIn0(OmsOrderMapper omsOrderMapper,
                                               OrderPayLogMapper orderPayLogMapper,
                                               OrderEventConsumeLogMapper orderEventConsumeLogMapper) {
        return event -> {
            if (event == null
                    || event.getPayNo() == null || event.getPayNo().isBlank()
                    || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
                return;
            }
            String result = event.getResult();
            if (result == null || result.isBlank()) {
                result = "SUCCESS";
            }
            result = result.toUpperCase();
            if (!"SUCCESS".equals(result) && !"FAIL".equals(result)) {
                result = "SUCCESS";
            }

            String eventType = "ORDER_PAY";

            int inserted = orderEventConsumeLogMapper.insertIgnore(event.getPayNo(), eventType);
            if (inserted == 0) {
                String status = orderEventConsumeLogMapper.selectStatus(event.getPayNo(), eventType);
                if ("DONE".equalsIgnoreCase(status)) {
                    return;
                }
            }

            String mappedOrderNo = orderPayLogMapper.selectOrderNoByPayNo(event.getPayNo());
            if (mappedOrderNo != null && !mappedOrderNo.equals(event.getOrderNo())) {
                orderEventConsumeLogMapper.markDone(event.getPayNo(), eventType);
                return;
            }
            if (mappedOrderNo == null) {
                orderPayLogMapper.insertIgnore(event.getPayNo(), event.getOrderNo());
            }

            if ("SUCCESS".equals(result)) {
                omsOrderMapper.update(
                        null,
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, event.getOrderNo())
                                .in(OmsOrder::getStatus, "WAIT_PAY","PAY_FAILED","PAYING")
                                .set(OmsOrder::getStatus, "CONFIRMED")
                                .set(OmsOrder::getFailReason, null)
                );
            } else {
                omsOrderMapper.update(
                        null,
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, event.getOrderNo())
                                .in(OmsOrder::getStatus, "WAIT_PAY","PAY_FAILED","PAYING")
                                .set(OmsOrder::getStatus, "PAY_FAILED")
                                .set(OmsOrder::getFailReason, "支付失败")
                );
            }

            OmsOrder latest = omsOrderMapper.selectOne(
                        new LambdaQueryWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, event.getOrderNo())
                                .last("limit 1")
                );
            if (latest == null ) {
                return;
            }
            String st = latest.getStatus();
            if ("CONFIRMED".equalsIgnoreCase(st)
                    || "CANCELLED".equalsIgnoreCase(st)
                    || "TIMEOUT_CANCELLED".equalsIgnoreCase(st)
                    || "FAILED".equalsIgnoreCase(st)
                    || "PAY_FAILED".equalsIgnoreCase(st)) {
                orderEventConsumeLogMapper.markDone(event.getPayNo(), eventType);
            }
        };
    }
}
