package com.lxylq7.client;

import com.lxylq7.common.Result;
import com.lxylq7.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "mall-product-service")
public interface ProductClient{

    @GetMapping("/products/{id}")
    Result<ProductDTO> getById(@PathVariable("id") Long id);
}
