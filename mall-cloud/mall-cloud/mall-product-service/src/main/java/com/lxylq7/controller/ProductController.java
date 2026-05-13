package com.lxylq7.controller;

import com.lxylq7.common.Result;
import com.lxylq7.dto.ProductDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class ProductController {

    @GetMapping("/products/{id}")
    public Result<ProductDTO> getById(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.fail("id不合法");
        }
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(id);
        productDTO.setName("测试商品-" + id);
        productDTO.setPrice(new BigDecimal("99.00"));
        productDTO.setStock(100);
        return Result.ok(productDTO);
    }
}
