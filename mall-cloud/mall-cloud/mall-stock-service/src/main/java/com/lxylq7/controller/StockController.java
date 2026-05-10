package com.lxylq7.controller;

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
    public StockDTO getStock(@PathVariable("productId") Long productId) {
        return stockService.getStock(productId);
    }

    @PostMapping("/stocks/deduct")
    public StockDeductResponse deduct(@RequestBody StockDeductRequest req) {return stockService.deduct(req);}

    @PostMapping("/stocks/release")
    public StockDeductResponse release(@RequestBody StockReleaseRequest req){return stockService.release(req);}
}
