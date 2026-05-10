package com.lxylq7.client;

import com.lxylq7.dto.StockDTO;
import com.lxylq7.dto.StockDeductRequest;
import com.lxylq7.dto.StockDeductResponse;
import com.lxylq7.dto.StockReleaseRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "mall-stock-service")
public interface StockClient {

    @GetMapping("/stocks/{productId}")
    StockDTO getStock(@PathVariable("productId") Long productId);

    @PostMapping("/stocks/deduct")
    StockDeductResponse deduct(@RequestBody StockDeductRequest req);

    @PostMapping("/stocks/release")
    StockDeductResponse release(@RequestBody StockReleaseRequest req);
}
