package com.lxylq7.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
public class OrderPayTimeoutJob {

    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private JobLockMapper jobLockMapper;

    @Value("${order.pay-timeout.enabled:true}")
    private boolean enabled;

    @Value("${order.pay-timeout.minutes:2}")
    private int minutes;

    @Value("${order.pay-timeout.limit:200}")
    private int limit;

    @Value("${order.pay-timeout.lock.name:order-pay-timeout}")
    private String lockName;

    @Value("${order.pay-timeout.lock.ttl:120000}")
    private long lockTtlMs;

    @Scheduled(fixedDelayString = "${order.pay-timeout.fixed-delay:60000}")
    public void payTimeout() {
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
                            .eq(OmsOrder::getStatus, "PAYING")
                            .isNotNull(OmsOrder::getCreateAt)
                            .lt(OmsOrder::getCreateAt, cutoff)
                            .last("limit " + limit)
            );

            for (OmsOrder o : candidates) {
                omsOrderMapper.update(
                        null,
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, o.getOrderNo())
                                .eq(OmsOrder::getStatus, "PAYING")
                                .set(OmsOrder::getStatus, "PAY_FAILED")
                                .set(OmsOrder::getFailReason, "支付超时")
                );
            }
        } finally {
            jobLockMapper.release(lockName, LocalDateTime.now());
        }
    }
}