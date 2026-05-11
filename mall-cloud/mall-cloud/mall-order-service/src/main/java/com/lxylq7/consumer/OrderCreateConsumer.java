package com.lxylq7.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.client.ProductClient;
import com.lxylq7.client.StockClient;
import com.lxylq7.client.UserClient;
import com.lxylq7.dto.*;
import com.lxylq7.entity.OmsOrder;
import com.lxylq7.mapper.OmsOrderMapper;
import com.lxylq7.mapper.OrderEventConsumeLogMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OrderCreateConsumer {

    @Bean
    public Consumer<OrderCreateEvent> orderCreateIn0(OmsOrderMapper omsOrderMapper,
                                                     UserClient userClient,
                                                     ProductClient productClient,
                                                     StockClient stockClient,
                                                     OrderEventConsumeLogMapper orderEventConsumeLogMapper) {
        return event -> {
            if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank() ||
            event.getBizNo() == null || event.getBizNo().isBlank()) {
                return;
            }
            //消息幂等
            int inserted = orderEventConsumeLogMapper.insertIgnore(event.getBizNo(), "ORDER_CREATE");
            if (inserted == 0) {
                return;
            }
            /*OmsOrder order = omsOrderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                    .eq(OmsOrder::getOrderNo, event.getOrderNo())
                    .last("limit 1"));
            if (order == null) {
                return;
            }*/
            //并发下只有一个能修改为PROCESSING状态 抢占式幂等
            int update = omsOrderMapper.update(
                    new LambdaUpdateWrapper<OmsOrder>()
                            .eq(OmsOrder::getOrderNo, event.getOrderNo())
                            .eq(OmsOrder::getStatus, "ACCEPTED")
                            .set(OmsOrder::getStatus, "PROCESSING")
                            .set(OmsOrder::getFailReason, null)
            );
            //没抢到 说明已经被其他消费者消费
            if (update == 0) {
                return;
            }
            //幂等 就不处理
            /*if ("CONFIRMED".equalsIgnoreCase(order.getStatus()) || "FAILED".equalsIgnoreCase(order.getStatus())) {
                return;
            }*/
            try {
                //用户校验
                UserDTO user = userClient.getById(event.getUserId());
                if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    failOrder(omsOrderMapper,event.getOrderNo(),"用户不存在或不可用");
                    return;
                }
                //商品校验
                ProductDTO product = productClient.getById(event.getProductId());
                if (product == null) {
                    failOrder(omsOrderMapper,event.getOrderNo(),"商品不存在");
                    return;
                }
            /*try {  测试用
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
                //库存扣减
                StockDeductRequest req = new StockDeductRequest();
                req.setProductId(event.getProductId());
                req.setQuantity(event.getQuantity());

                StockDeductResponse deduct = stockClient.deduct(req);
                if (deduct == null || !Boolean.TRUE.equals(deduct.getSuccess())) {
                    String reason = (deduct == null || deduct.getMessage() == null || deduct.getMessage().isBlank())
                            ? "库存服务异常"
                            : deduct.getMessage();
                    failOrder(omsOrderMapper, event.getOrderNo(), reason);
                    return;
                }
                //成功 订单确认
                omsOrderMapper.update(
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, event.getOrderNo())
                                .eq(OmsOrder::getStatus, "PROCESSING")
                                .set(OmsOrder::getStatus,"CONFIRMED")
                                .set(OmsOrder::getFailReason,null)
                );
            } catch (Exception e) {
                failOrder(omsOrderMapper, event.getOrderNo(), "消费异常:" + e.getMessage());
            }
        };
    }

    private void failOrder(OmsOrderMapper omsOrderMapper, String orderNo, String reason) {
        omsOrderMapper.update(
                null,
                new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .eq(OmsOrder::getStatus, "PROCESSING")
                        .set(OmsOrder::getStatus, "FAILED")
                        .set(OmsOrder::getFailReason, reason)
        );
    }
}
