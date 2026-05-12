package com.lxylq7.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.client.StockClient;
import com.lxylq7.dto.StockReleaseRequest;
import com.lxylq7.entity.OmsOrder;
import com.lxylq7.mapper.JobLockMapper;
import com.lxylq7.mapper.OmsOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderProcessingTimeOutFailJob {

    @Autowired
    private  OmsOrderMapper omsOrderMapper;
    @Autowired
    private  JobLockMapper jobLockMapper;
    @Autowired
    private  StockClient stockClient;

    @Value("${order.processing-timeout.enabled:true}")
    private boolean enabled;

    @Value("${order.processing-timeout.minutes:2}")
    private int minutes;

    @Value("${order.processing-timeout.limit:200}")
    private int limit;

    @Value("${order.processing-timeout.lock.name:order-processing-timeout}")
    private String lockName;

    @Value("${order.processing-timeout.lock.ttl:120000}")
    private long lockTtlMs;

    @Scheduled(fixedDelayString = "${order.processing-timeout.fixed-delay:60000}")
    public void failTimeoutProcessingOrders() {
        if (!enabled) {
            return;
        }

        if (minutes <= 0) minutes = 2;
        if (minutes > 24 * 60) minutes = 24 * 60;
        if (limit <= 0) limit = 200;
        if (limit > 2000) limit = 2000;
        if (lockTtlMs <= 0) lockTtlMs = 120000;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusNanos(lockTtlMs * 1_000_000L);

        int locked = jobLockMapper.tryLock(lockName, now, until);
        if (locked <= 0) {
            return;
        }

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);

            List<OmsOrder> candidates = omsOrderMapper.selectList(
                    new LambdaQueryWrapper<OmsOrder>()
                            .eq(OmsOrder::getStatus, "PROCESSING")
                            .isNotNull(OmsOrder::getCreateAt)
                            .lt(OmsOrder::getCreateAt, cutoff)
                            .last("limit " + limit)
            );

            for (OmsOrder o : candidates) {
                int rows = omsOrderMapper.update(
                        null,
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, o.getOrderNo())
                                .eq(OmsOrder::getStatus, "PROCESSING")
                                .set(OmsOrder::getStatus, "FAILED")
                                .set(OmsOrder::getFailReason, "处理超时")
                );

                if (rows > 0 && o.getStockDeducted() != null && o.getStockDeducted() == 1) {
                    try {
                        StockReleaseRequest req = new StockReleaseRequest();
                        req.setProductId(o.getProductId());
                        req.setQuantity(o.getQuantity());
                        stockClient.release(req);
                    } catch (Exception ignored) {
                    }
                }
            }
        } finally {
            jobLockMapper.release(lockName, LocalDateTime.now());
        }
    }
}
