package com.lxylq7.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.lxylq7.client.ProductClient;
import com.lxylq7.client.StockClient;
import com.lxylq7.client.UserClient;
import com.lxylq7.dto.*;
import com.lxylq7.entity.OmsOrder;
import com.lxylq7.mapper.OmsOrderMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OrderCreateConsumer {

    @Bean
    public Consumer<OrderCreateEvent> orderCreateIn0(OmsOrderMapper omsOrderMapper,
                                                     UserClient userClient,
                                                     ProductClient productClient,
                                                     StockClient stockClient) {
        return event -> {
            if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
                return;
            }
            OmsOrder order = omsOrderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                    .eq(OmsOrder::getOrderNo, event.getOrderNo())
                    .last("limit 1"));
            if (order == null) {
                return;
            }
            //幂等 就不处理
            if ("CONFIRMED".equalsIgnoreCase(order.getStatus()) || "FAILED".equalsIgnoreCase(order.getStatus())) {
                return;
            }
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

            //库存扣减
            StockDeductRequest req = new StockDeductRequest();
            req.setProductId(event.getProductId());
            req.setQuantity(event.getQuantity());

            StockDeductResponse deduct = stockClient.deduct(req);
            if (deduct == null || !Boolean.TRUE.equals(deduct.getSuccess())) {
                failOrder(omsOrderMapper,event.getOrderNo(),"库存服务异常");
                return;
            }
            //成功 订单确认
            omsOrderMapper.update(
                    new LambdaUpdateWrapper<OmsOrder>()
                            .eq(OmsOrder::getOrderNo, event.getOrderNo())
                            .set(OmsOrder::getStatus,"CONFIRMED")
                            .set(OmsOrder::getFailReason,null)
            );
        };
    }

    private void failOrder(OmsOrderMapper omsOrderMapper, String orderNo, String reason) {
        omsOrderMapper.update(
                null,
                new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .set(OmsOrder::getStatus, "FAILED")
                        .set(OmsOrder::getFailReason, reason)
        );
    }
}
