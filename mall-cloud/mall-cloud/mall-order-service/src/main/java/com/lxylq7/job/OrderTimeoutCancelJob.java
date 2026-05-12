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
public class OrderTimeoutCancelJob {

    @Autowired
    private  OmsOrderMapper omsOrderMapper; //操作订单表
    @Autowired
    private JobLockMapper jobLockMapper;  //分布式锁

    @Value("${order.timeout-cancel.enabled:true}")
    private boolean enabled;

    @Value("${order.timeout-cancel.minutes:15}")
    private int minutes;

    @Value("${order.timeout-cancel.limit:200}")
    private int limit;

    @Value("${order.timeout-cancel.lock.name:order-timeout-cancel}")
    private String lockName;

    @Value("${order.timeout-cancel.lock.ttl:120000}")
    private long lockTtlMs;

    @Scheduled(fixedDelayString = "${order.timeout-cancel.fixed-delay:60000}")
    public void timeoutCancel() {
        if (!enabled) {
            return;
        }

        if (minutes <= 0) minutes = 15;
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
                            .eq(OmsOrder::getStatus, "ACCEPTED")
                            .isNotNull(OmsOrder::getCreateAt)
                            .lt(OmsOrder::getCreateAt, cutoff)
                            .last("limit " + limit)
            );

            for (OmsOrder o : candidates) {
                omsOrderMapper.update(
                        null,
                        new LambdaUpdateWrapper<OmsOrder>()
                                .eq(OmsOrder::getOrderNo, o.getOrderNo())
                                .eq(OmsOrder::getStatus, "ACCEPTED")
                                .set(OmsOrder::getStatus, "TIMEOUT_CANCELLED")
                                .set(OmsOrder::getFailReason, "超时取消")
                );
            }
        } finally {
            jobLockMapper.release(lockName, LocalDateTime.now());
        }
    }
}