package com.lxylq7.client;

import com.lxylq7.dto.StockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mall-stock-service")
public interface StockClient {

    @GetMapping("/stocks/{productId}")
    StockDTO getStock(@PathVariable("productId") Long productId);
}
