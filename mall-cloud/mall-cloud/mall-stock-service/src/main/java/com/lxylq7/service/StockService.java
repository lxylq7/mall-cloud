package com.lxylq7.service;

import com.lxylq7.dto.StockDTO;
import com.lxylq7.dto.StockDeductRequest;
import com.lxylq7.dto.StockDeductResponse;
import com.lxylq7.dto.StockReleaseRequest;

public interface StockService {

    StockDTO getStock(Long productId);
    StockDeductResponse deduct(StockDeductRequest req);
    StockDeductResponse release(StockReleaseRequest req);

}
