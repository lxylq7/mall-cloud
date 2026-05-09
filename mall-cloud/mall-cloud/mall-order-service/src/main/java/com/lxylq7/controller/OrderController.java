package com.lxylq7.controller;

import com.lxylq7.client.ProductClient;
import com.lxylq7.client.StockClient;
import com.lxylq7.client.UserClient;
import com.lxylq7.dto.*;
import com.lxylq7.entity.OmsOrder;
import com.lxylq7.mapper.OmsOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class OrderController {

    @Autowired
    private ProductClient productClient;
    @Autowired
    private StockClient stockClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private OmsOrderMapper omsOrderMapper;

    @PostMapping("/orders")
    public OrderCreateResponse create(@RequestBody OrderCreateRequest req) {
        OrderCreateResponse resp = new OrderCreateResponse();

        if (req == null || req.getUserId() == null || req.getProductId() == null || req.getQuantity() == null) {
            resp.setSuccess(false);
            resp.setMessage("userId/productId/quantity不能为空");
        }
        UserDTO user = userClient.getById(req.getUserId());
        if (user == null || !"active".equalsIgnoreCase(user.getStatus())){ //忽略大小写
            resp.setSuccess(false);
            resp.setMessage("用户不存在或者不可用");
            return resp;
        }
        ProductDTO product = productClient.getById(req.getProductId());
        if (product == null) {
            resp.setSuccess(false);
            resp.setMessage("商品不存在");
            return resp;
        }
        StockDTO stock = stockClient.getStock(req.getProductId());
        if (stock == null) {
            resp.setSuccess(false);
            resp.setMessage("库存信息不存在");
            return resp;
        }

        if (req.getQuantity() > stock.getAvailable()) {
            resp.setSuccess(false);
            resp.setMessage("库存不足");
            resp.setOrderNo("MOCK-" + System.currentTimeMillis());
            resp.setUserId(req.getUserId());
            resp.setProduct(product);
            resp.setQuantity(req.getQuantity());
            return resp;
        }
        //插入到数据库
        String orderNo = "MOCK-" + System.currentTimeMillis();
        OmsOrder order = new OmsOrder();
        order.setOrderNo(orderNo);
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setQuantity(req.getQuantity());
        order.setStatus("CREATED");
        //order.setCreateAt(LocalDateTime.now());  不写也可以 表结构默认值会兜底
        omsOrderMapper.insert(order);

        resp.setSuccess(true);
        resp.setMessage("下单成功(模拟)");
        resp.setOrderNo("MOCK-" + System.currentTimeMillis());
        resp.setUserId(req.getUserId());
        resp.setProduct(product);
        resp.setQuantity(req.getQuantity());
        return resp;
    }
}
