package com.lxylq7.controller;

import com.lxylq7.common.Result;
import com.lxylq7.dto.StockDTO;
import com.lxylq7.dto.StockDeductRequest;
import com.lxylq7.dto.StockDeductResponse;
import com.lxylq7.dto.StockReleaseRequest;
import com.lxylq7.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.bind.annotation.*;

@RestController
public class StockController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisScript<Long> redisScript;
    @Autowired
    private StockService stockService;

    @GetMapping("/stocks/{productId}")
    public Result<StockDTO> getStock(@PathVariable("productId") Long productId) {
        StockDTO dto = stockService.getStock(productId);
        if (dto == null) {
            return Result.fail("库存不存在");
        }
        return Result.ok(dto);
    }

    @PostMapping("/stocks/deduct")
    public Result<StockDeductResponse> deduct(@RequestBody StockDeductRequest req) {
        StockDeductResponse resp = stockService.deduct(req);
        if (resp == null) {
            return Result.fail("库存服务异常");
        }
        if (Boolean.TRUE.equals(resp.getSuccess())) {
            return Result.ok(resp.getMessage() == null ? "OK" : resp.getMessage(), resp);
        }
        return Result.fail(resp.getMessage() == null ? "扣减失败" : resp.getMessage());
    }

    @PostMapping("/stocks/release")
    public Result<StockDeductResponse> release(@RequestBody StockReleaseRequest req) {
        StockDeductResponse resp = stockService.release(req);
        if (resp == null) {
            return Result.fail("库存服务异常");
        }
        if (Boolean.TRUE.equals(resp.getSuccess())) {
            return Result.ok(resp.getMessage() == null ? "OK" : resp.getMessage(), resp);
        }
        return Result.fail(resp.getMessage() == null ? "释放失败" : resp.getMessage());
    }
}
