package com.lxylq7.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.common.Result;
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
            String eventType = "ORDER_CREATE";
            //消息幂等
            int inserted = orderEventConsumeLogMapper.insertIgnore(event.getBizNo(), eventType);
            if (inserted == 0) {
                String status = orderEventConsumeLogMapper.selectStatus(event.getBizNo(), eventType);
                if ("DONE".equalsIgnoreCase(status)) {
                    return;
                }
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
                //用户很快点击取消后 订单表里会是CANCELLED状态 但是订单事件表里会是PROCESSING状态 所以需要判断一下
                OmsOrder existing = omsOrderMapper.selectOne(
                        new LambdaQueryWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, event.getOrderNo())
                                .last("limit 1")
                );
                if (existing == null
                        || "CONFIRMED".equalsIgnoreCase(existing.getStatus())
                        || "FAILED".equalsIgnoreCase(existing.getStatus())
                        || "CANCELLED".equalsIgnoreCase(existing.getStatus())
                        || "TIMEOUT_CANCELLED".equalsIgnoreCase(existing.getStatus())
                        || "WAIT_PAY".equalsIgnoreCase(existing.getStatus())) {
                    orderEventConsumeLogMapper.markDone(event.getBizNo(), eventType);
                }
                return;
            }
            //幂等 就不处理
            /*if ("CONFIRMED".equalsIgnoreCase(order.getStatus()) || "FAILED".equalsIgnoreCase(order.getStatus())) {
                return;
            }*/
            boolean deducted = false;
            try {
                //用户校验
                Result<UserDTO> userResult = userClient.getById(event.getUserId());
                UserDTO user = userResult == null ? null : userResult.getData();
                if (userResult == null || !userResult.isSuccess() || user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    failOrder(omsOrderMapper,event.getOrderNo(),"用户不存在或不可用");
                    orderEventConsumeLogMapper.markDone(event.getBizNo(), eventType);
                    return;
                }
                //商品校验
                Result<ProductDTO> productResult = productClient.getById(event.getProductId());
                ProductDTO product = productResult == null ? null : productResult.getData();
                if (productResult == null || !productResult.isSuccess() || product == null) {
                    failOrder(omsOrderMapper,event.getOrderNo(),"商品不存在");
                    orderEventConsumeLogMapper.markDone(event.getBizNo(), eventType);
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

                Result<StockDeductResponse> deductResult = stockClient.deduct(req);
                StockDeductResponse deduct = deductResult == null ? null : deductResult.getData();
                if (deductResult == null || !deductResult.isSuccess() || deduct == null || !Boolean.TRUE.equals(deduct.getSuccess())) {
                    String reason = deductResult == null
                            ? "库存服务异常"
                            : (deductResult.getMessage() == null || deductResult.getMessage().isBlank() ? "扣减失败" : deductResult.getMessage());
                    failOrder(omsOrderMapper, event.getOrderNo(), reason);
                    orderEventConsumeLogMapper.markDone(event.getBizNo(), eventType);
                    return;
                }
                deducted = true;
                //给订单表更新库存扣减数量
                omsOrderMapper.update(
                        null,
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, event.getOrderNo())
                                .eq(OmsOrder::getStatus, "PROCESSING")
                                .set(OmsOrder::getStockDeducted,1)
                );
                //成功 订单确认
                omsOrderMapper.update(
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, event.getOrderNo())
                                .eq(OmsOrder::getStatus, "PROCESSING")
                                .set(OmsOrder::getStatus,"WAIT_PAY")
                                .set(OmsOrder::getFailReason,null)
                );
                orderEventConsumeLogMapper.markDone(event.getBizNo(),eventType);
            } catch (Exception e) {
                failOrder(omsOrderMapper, event.getOrderNo(), "消费异常:" + e.getMessage());
                if (deducted){
                    try {
                        //库存回滚
                        StockReleaseRequest releaseRq = new StockReleaseRequest();
                        releaseRq.setProductId(event.getProductId());
                        releaseRq.setQuantity(event.getQuantity());
                        stockClient.release(releaseRq);
                    } catch (Exception ex) {
                    }
                }
                orderEventConsumeLogMapper.markDone(event.getBizNo(), eventType);
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
