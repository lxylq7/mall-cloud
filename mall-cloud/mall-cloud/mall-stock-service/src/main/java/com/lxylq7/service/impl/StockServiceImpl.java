package com.lxylq7.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.dto.StockDTO;
import com.lxylq7.dto.StockDeductRequest;
import com.lxylq7.dto.StockDeductResponse;
import com.lxylq7.dto.StockReleaseRequest;
import com.lxylq7.entity.WmsStock;
import com.lxylq7.mapper.WmsStockMapper;
import com.lxylq7.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class StockServiceImpl implements StockService {

    private static final long STOCK_CACHE_TTL_MINUTES = 30L;
    private static final String STOCK_KEY_PREFIX = "stock:available:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private WmsStockMapper wmsStockMapper;
    @Autowired
    private RedisScript<Long> redisScript;
    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public StockDTO getStock(Long productId) {
        //先查缓存
        String key = STOCK_KEY_PREFIX + productId;
        String val = stringRedisTemplate.opsForValue().get(key);
        Integer available;
        if (val != null) {
            available = Integer.parseInt(val);
        } else {
            //缓存未命中 查数据库
            WmsStock stock = wmsStockMapper.selectOne(
                    new LambdaQueryWrapper<WmsStock>()
                            .eq(WmsStock::getProductId,productId)
                            .last("limit 1")
            );
            if (stock == null) {
                available = 0;
            } else {
                available = stock.getAvailable();
            }
            //写缓存
            stringRedisTemplate.opsForValue().set(
                    key,
                    String.valueOf(available),
                    STOCK_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        }
        StockDTO stockDTO = new StockDTO();
        stockDTO.setProductId(productId);
        stockDTO.setAvailable(available);
        stockDTO.setLocked(0);
        return stockDTO;
    }


    @Override
    public StockDeductResponse deduct(StockDeductRequest req) {
        StockDeductResponse resp = new StockDeductResponse();

        if (req == null || req.getProductId() == null || req.getQuantity() == null || req.getQuantity() <= 0) {
            resp.setSuccess(false);
            resp.setMessage("参数非法");
            return resp;
        }
        String key = STOCK_KEY_PREFIX + req.getProductId();
        Long ret = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(key), //创建一个只有一个元素的集合
                String.valueOf(req.getQuantity())
        );
        if (ret == null || ret == -1) {
            resp.setSuccess(false);
            resp.setMessage("库存未初始化");
        } else if (ret == 0) {
            resp.setSuccess(false);
            resp.setMessage("库存不足");
        } else {
            //redis扣减成功后 更新数据库
            int rows = wmsStockMapper.update(
                    null,
                    new LambdaUpdateWrapper<WmsStock>()
                            .eq(WmsStock::getProductId, req.getProductId())
                            .ge(WmsStock::getAvailable, req.getQuantity())
                            .setSql("available = available - " + req.
                                    getQuantity())
                            .setSql("locked = locked + " + req.getQuantity())
            );
            //db更新失败 回滚redis
            if (rows == 0) {
                stringRedisTemplate.opsForValue().increment(key, req.getQuantity().longValue());
                resp.setSuccess(false);
                resp.setMessage("数据库扣减失败,已回滚缓存");
            } else {
                resp.setSuccess(true);
                resp.setMessage("扣减成功");
            }
        }
        return resp;
    }

    /**
     * 失败补偿库存
     * @param req
     * @return
     */
    @Override
    public StockDeductResponse release(StockReleaseRequest req) {
        StockDeductResponse resp = new StockDeductResponse();
        if (req == null || req.getProductId() == null ||
        req.getQuantity() == null || req.getQuantity() <= 0){
            resp.setSuccess(false);
            resp.setMessage("参数非法");
            return resp;
        }
        //先改数据库 避免redis先加但db没加
        int rows = wmsStockMapper.update(
                null,
                new LambdaUpdateWrapper<WmsStock>()
                        .eq(WmsStock::getProductId,req.getProductId())
                        .ge(WmsStock::getAvailable,req.getQuantity())
                        .setSql("available = available + " + req.getQuantity()
                        + "locked = locked - " + req.getQuantity())
        );
        if (rows == 0) {
            //失败
            resp.setSuccess(false);
            resp.setMessage("数据库回补失败");
            return resp;
        }
        //再回补redis
        String key = STOCK_KEY_PREFIX + req.getProductId();
        stringRedisTemplate.opsForValue().increment(key,req.getQuantity().longValue());
        resp.setSuccess(true);
        resp.setMessage("回补成功");
        return resp;
    }


}
