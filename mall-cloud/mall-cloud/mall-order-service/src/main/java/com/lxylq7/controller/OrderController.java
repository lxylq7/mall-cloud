package com.lxylq7.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.client.ProductClient;
import com.lxylq7.client.StockClient;
import com.lxylq7.client.UserClient;
import com.lxylq7.dto.*;
import com.lxylq7.entity.OmsOrder;
import com.lxylq7.mapper.OmsOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private org.springframework.cloud.stream.function.StreamBridge streamBridge;

    @PostMapping("/orders")
    public OrderCreateResponse create(@RequestBody OrderCreateRequest req) {
        OrderCreateResponse resp = new OrderCreateResponse();

        if (req == null || req.getUserId() == null || req.getProductId() == null || req.getQuantity() == null) {
            resp.setSuccess(false);
            resp.setMessage("userId/productId/quantity不能为空");
            return resp;
        }
        /*UserDTO user = userClient.getById(req.getUserId());
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

        StockDeductRequest stockDeductRequest = new StockDeductRequest();
        stockDeductRequest.setProductId(req.getProductId());
        stockDeductRequest.setQuantity(req.getQuantity());

        StockDeductResponse deduct = stockClient.deduct(stockDeductRequest);
        if (deduct == null || !Boolean.TRUE.equals(deduct.getSuccess())){
            resp.setSuccess(false);
            resp.setMessage(deduct == null ? "库存服务异常" : deduct.getMessage());
            return resp;
        }*/

        //插入到数据库
        String orderNo = "MOCK-" + System.currentTimeMillis();
        //String orderNo = "MOCK-FIXED-001";  用来测试release接口的
        OmsOrder order = new OmsOrder();
        order.setOrderNo(orderNo);
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setQuantity(req.getQuantity());
        order.setStatus("ACCEPTED");
        //order.setCreateAt(LocalDateTime.now());  不写也可以 表结构默认值会兜底
        try {
            omsOrderMapper.insert(order);
        } catch (Exception e) {
            //订单失败 -> 回补库存
            resp.setSuccess(false);
            resp.setMessage("订单受理失败:"+e.getMessage());
            return resp;
            /*StockReleaseRequest releaseReq = new StockReleaseRequest();
            try {
                releaseReq.setProductId(req.getProductId());
                releaseReq.setQuantity(req.getQuantity());
                stockClient.release(releaseReq);
            } catch (Exception ex) {
                resp.setSuccess(false);
                resp.setMessage("回补异常:" + ex.getMessage());
                return resp;
            }
            resp.setSuccess(false);
            resp.setMessage("订单创建失败,库存已回补");
            return resp;*/
        }
        //发送下单消息到mq
        OrderCreateEvent event = new OrderCreateEvent();
        event.setBizNo(java.util.UUID.randomUUID().toString());
        event.setQuantity(req.getQuantity());
        event.setProductId(req.getProductId());
        event.setUserId(req.getUserId());
        event.setOrderNo(orderNo);
        event.setTs(System.currentTimeMillis());

        boolean sent = streamBridge.send("orderCreateOut0", event);
        if (!sent) {
            //mq发送失败
            omsOrderMapper.update(
                    new LambdaUpdateWrapper<OmsOrder>()
                            .eq(OmsOrder::getOrderNo, orderNo)
                            .set(OmsOrder::getStatus, "FAILED")
                            .set(OmsOrder::getFailReason, "MQ发送失败")
            );
            resp.setSuccess(false);
            resp.setMessage("订单受理失败:MQ发送失败");
            resp.setOrderNo(orderNo);
            return resp;
        }

        resp.setSuccess(true);
        resp.setMessage("订单已经受理");
        resp.setOrderNo(orderNo);
        resp.setUserId(req.getUserId());
        resp.setQuantity(req.getQuantity());
        return resp;
    }

    /**
     * 查询订单详情
     * @return
     */
    @GetMapping("/orders/{orderNo}")
    public OrderCreateResponse query(@PathVariable("orderNo") String orderNo) {
        OrderCreateResponse resp = new OrderCreateResponse();
        OmsOrder order = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (order == null) {
            resp.setSuccess(false);
            resp.setMessage("订单不存在");
            return resp;
        }
        resp.setSuccess(true);
        resp.setOrderNo(orderNo);
        resp.setQuantity(order.getQuantity());
        resp.setUserId(order.getUserId());

        if ("ACCEPTED".equalsIgnoreCase(order.getStatus())) {
            resp.setMessage("处理中");
        } else if ("CONFIRMED".equals(order.getStatus())) {
            resp.setMessage("下单成功");
        } else if ("CANCELLED".equals(order.getStatus())) {
            resp.setMessage("已取消");
        } else if ("TIMEOUT_CANCELLED".equals(order.getStatus())) {
            resp.setMessage("超时取消");
        } else if ("WAIT_PAY".equals(order.getStatus())) {
            resp.setMessage("待支付");
        } else if ("FAILED".equals(order.getStatus())) {
            resp.setMessage(order.getFailReason() == null ? "下单失败" : order.getFailReason());
        } else {
            resp.setMessage(order.getStatus());
        }
        return resp;
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public OrderCreateResponse cancel(@PathVariable("orderNo") String orderNo) {
        OrderCreateResponse resp = new OrderCreateResponse();

        if (orderNo == null || orderNo.isBlank()) {
            resp.setSuccess(false);
            resp.setMessage("orderNo不能为空");
            return resp;
        }

        int rows = omsOrderMapper.update(
                null,
                new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .eq(OmsOrder::getStatus, "WAIT_PAY")
                        .set(OmsOrder::getStatus, "CANCELLED")
                        .set(OmsOrder::getFailReason, "用户取消")
        );

        if (rows > 0) {
            OmsOrder order = omsOrderMapper.selectOne(
                    new LambdaQueryWrapper<OmsOrder>()
                            .eq(OmsOrder::getOrderNo, orderNo)
                            .last("limit 1")
            );
            if (order != null && order.getStockDeducted() != null && order.getStockDeducted() == 1) {
                try {
                    StockReleaseRequest req = new StockReleaseRequest();
                    req.setProductId(order.getProductId());
                    req.setQuantity(order.getQuantity());
                    stockClient.release(req);
                } catch (Exception ignored) {
                }
            }

            resp.setSuccess(true);
            resp.setOrderNo(orderNo);
            resp.setMessage("已取消");
            return resp;
        }

        OmsOrder order = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (order == null) {
            resp.setSuccess(false);
            resp.setMessage("订单不存在");
            return resp;
        }

        resp.setSuccess(false);
        resp.setOrderNo(orderNo);

        if ("CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            resp.setMessage("订单已成功，不可取消");
        } else if ("FAILED".equalsIgnoreCase(order.getStatus())) {
            resp.setMessage("订单已失败，无需取消");
        } else if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "TIMEOUT_CANCELLED".equalsIgnoreCase(order.getStatus())) {
            resp.setMessage("订单已取消");
        } else {
            resp.setMessage("订单处理中，不可取消");
        }
        return resp;
    }

    @PostMapping("/orders/timeout-cancel")
    public Map<String, Object> timeoutCancel(
            @RequestParam(value = "minutes", defaultValue = "15") int minutes,
            @RequestParam(value = "limit", defaultValue = "200") int limit
    ) {
        //参数合法性校验
        if (minutes <= 0) minutes = 15;
        if (minutes > 24 * 60) minutes = 24 * 60;
        if (limit <= 0) limit = 200;
        if (limit > 2000) limit = 2000;

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);

        List<OmsOrder> candidates = omsOrderMapper.selectList(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getStatus, "WAIT_PAY")
                        .isNotNull(OmsOrder::getCreateAt)
                        .lt(OmsOrder::getCreateAt, cutoff)
                        .last("limit " + limit)
        );

        int updated = 0;
        for (OmsOrder o : candidates) {
            int rows = omsOrderMapper.update(
                    null,
                    new LambdaUpdateWrapper<OmsOrder>()
                            .eq(OmsOrder::getOrderNo, o.getOrderNo())
                            .eq(OmsOrder::getStatus, "WAIT_PAY")
                            .set(OmsOrder::getStatus, "TIMEOUT_CANCELLED")
                            .set(OmsOrder::getFailReason, "超时取消")
            );
            if (rows > 0 && o.getStockDeducted() != null && o.getStockDeducted() == 1) {
                try {
                    StockReleaseRequest req = new StockReleaseRequest();
                    req.setProductId(o.getProductId());
                    req.setQuantity(o.getQuantity());
                    stockClient.release(req);
                } catch (Exception ignored) {
                }
            }
            updated += rows;
        }

        return Map.of(
                "success", true,
                "minutes", minutes,
                "cutoff", cutoff.toString(),
                "scanned", candidates.size(),
                "updated", updated
        );
    }

    @PostMapping("/orders/{orderNo}/pay")
    public OrderCreateResponse pay(@PathVariable("orderNo") String orderNo) {
        OrderCreateResponse resp = new OrderCreateResponse();

        if (orderNo == null || orderNo.isBlank()) {
            resp.setSuccess(false);
            resp.setMessage("orderNo不能为空");
            return resp;
        }

        int rows = omsOrderMapper.update(
                null,
                new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .eq(OmsOrder::getStatus, "WAIT_PAY")
                        .set(OmsOrder::getStatus, "CONFIRMED")
                        .set(OmsOrder::getFailReason, null)
        );

        if (rows > 0) {
            resp.setSuccess(true);
            resp.setOrderNo(orderNo);
            resp.setMessage("支付成功");
            return resp;
        }

        OmsOrder order = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (order == null) {
            resp.setSuccess(false);
            resp.setMessage("订单不存在");
            return resp;
        }

        resp.setSuccess(false);
        resp.setOrderNo(orderNo);
        resp.setMessage("当前状态不可支付:" + order.getStatus());
        return resp;
    }
}
