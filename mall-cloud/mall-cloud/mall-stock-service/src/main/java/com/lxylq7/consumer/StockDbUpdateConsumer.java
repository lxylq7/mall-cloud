package com.lxylq7.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.dto.StockChangeEvent;
import com.lxylq7.entity.WmsStock;
import com.lxylq7.mapper.StockEventConsumerLogMapper;
import com.lxylq7.mapper.WmsStockMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class StockDbUpdateConsumer {

    @Bean
    public Consumer<StockChangeEvent> stockIn0(WmsStockMapper wmsStockMapper,
                                               StockEventConsumerLogMapper
                                               consumerLogMapper) {
        return event -> {
            if (event == null || event.getBizNo() == null || event.getBizNo().isBlank()
                    || event.getProductId() == null || event.getQuantity() == null ||
                    event.getQuantity() <= 0) {
                return;
            }
            //幂等抢占 同一个bizno 只允许第一次处理
            int inserted = consumerLogMapper.insertIgnore(event.getBizNo(), event.getType());
            if (inserted == 0) {
                return; //已消费过 直接忽略
            }
            if ("DEDUCT".equalsIgnoreCase(event.getType())) {
                wmsStockMapper.update(
                        null,
                        new LambdaUpdateWrapper<WmsStock>()
                                .eq(WmsStock::getProductId,event.getProductId())
                                .ge(WmsStock::getAvailable,event.getQuantity())
                                .setSql("available = available - " + event.getQuantity()
                                + ", `locked` = `locked` + " + event.getQuantity())
                );
            } else if ("RELEASE".equalsIgnoreCase(event.getType())) {
                wmsStockMapper.update(
                        null,
                        new LambdaUpdateWrapper<WmsStock>()
                                .eq(WmsStock::getProductId,event.getProductId())
                                .ge(WmsStock::getLocked,event.getQuantity())
                                .setSql("available = available + " + event.getQuantity()
                                        + ", `locked` = `locked` - " + event.getQuantity())
                );
            }
        };
    }
}
