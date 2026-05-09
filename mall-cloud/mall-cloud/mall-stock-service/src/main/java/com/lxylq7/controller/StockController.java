package com.lxylq7.controller;

import com.lxylq7.dto.StockDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class StockController {
    @GetMapping("/stocks/{productId}")
    public StockDTO getStock(@PathVariable("productId") Long productId) {
        StockDTO stockDTO = new StockDTO();
        stockDTO.setProductId(productId);
        stockDTO.setAvailable(88);
        stockDTO.setLocked(0);
        return stockDTO;
    }
}
