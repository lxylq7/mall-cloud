package com.lxylq7.service;

import com.lxylq7.dto.StockDTO;
import com.lxylq7.dto.StockDeductRequest;
import com.lxylq7.dto.StockDuductResponse;

public interface StockService {

    StockDTO getStock(Long productId);
    StockDuductResponse deduct(StockDeductRequest req);

}
