package com.lxylq7.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lxylq7.client.ProductClient;
import com.lxylq7.client.StockClient;
import com.lxylq7.client.UserClient;
import com.lxylq7.common.Result;
import com.lxylq7.dto.*;
import jakarta.validation.Valid;
import com.lxylq7.entity.OmsOrder;
import com.lxylq7.mapper.OmsOrderMapper;
import com.lxylq7.mapper.OrderPayLogMapper;
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
    private OrderPayLogMapper orderPayLogMapper;
    @Autowired
    private org.springframework.cloud.stream.function.StreamBridge streamBridge;

    @PostMapping("/orders")
    public Result<OrderCreateResponse> create(@Valid @RequestBody OrderCreateRequest req) {
        if (req == null || req.getUserId() == null || req.getProductId() == null || req.getQuantity() == null) {
            return Result.fail("userId/productId/quantity不能为空");
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
            OrderCreateResponse data = new OrderCreateResponse();
            data.setOrderNo(orderNo);
            data.setUserId(req.getUserId());
            data.setQuantity(req.getQuantity());
            return Result.fail("订单受理失败:" + e.getMessage(), data);
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
            OrderCreateResponse data = new OrderCreateResponse();
            data.setOrderNo(orderNo);
            data.setUserId(req.getUserId());
            data.setQuantity(req.getQuantity());
            return Result.fail("订单受理失败:MQ发送失败", data);
        }

        OrderCreateResponse data = new OrderCreateResponse();
        data.setOrderNo(orderNo);
        data.setUserId(req.getUserId());
        data.setQuantity(req.getQuantity());
        return Result.ok("订单已经受理", data);
    }

    /**
     * 查询订单详情
     * @return
     */
    @GetMapping("/orders/{orderNo}")
    public Result<OrderCreateResponse> query(@PathVariable("orderNo") String orderNo) {
        OmsOrder order = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (order == null) {
            return Result.fail("订单不存在");
        }
        OrderCreateResponse data = new OrderCreateResponse();
        data.setOrderNo(orderNo);
        data.setQuantity(order.getQuantity());
        data.setUserId(order.getUserId());

        String message;
        if ("ACCEPTED".equalsIgnoreCase(order.getStatus())) {
            message = "处理中";
        } else if ("CONFIRMED".equals(order.getStatus())) {
            message = "下单成功";
        } else if ("CANCELLED".equals(order.getStatus())) {
            message = "已取消";
        } else if ("TIMEOUT_CANCELLED".equals(order.getStatus())) {
            message = "超时取消";
        } else if ("WAIT_PAY".equals(order.getStatus())) {
            message = "待支付";
        } else if ("PAY_FAILED".equals(order.getStatus())) {
            message = "支付失败,可重新支付或取消";
        } else if ("PAYING".equals(order.getStatus())) {
            message = "支付中";
        } else if ("FAILED".equals(order.getStatus())) {
            message = order.getFailReason() == null ? "下单失败" : order.getFailReason();
        } else {
            message = order.getStatus();
        }
        return Result.ok(message, data);
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public Result<OrderCreateResponse> cancel(@PathVariable("orderNo") String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Result.fail("orderNo不能为空");
        }

        int rows = omsOrderMapper.update(
                null,
                new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .in(OmsOrder::getStatus, "WAIT_PAY","PAY_FAILED","PAYING")
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

            OrderCreateResponse data = new OrderCreateResponse();
            data.setOrderNo(orderNo);
            return Result.ok("已取消", data);
        }

        OmsOrder order = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (order == null) {
            return Result.fail("订单不存在");
        }

        OrderCreateResponse data = new OrderCreateResponse();
        data.setOrderNo(orderNo);

        String message;
        if ("CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            message = "订单已成功，不可取消";
        } else if ("FAILED".equalsIgnoreCase(order.getStatus())) {
            message = "订单已失败，无需取消";
        } else if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "TIMEOUT_CANCELLED".equalsIgnoreCase(order.getStatus())) {
            message = "订单已取消";
        } else if ("PAY_FAILED".equalsIgnoreCase(order.getStatus())) {
            message = "支付失败,可重新支付或取消";
        } else if ("WAIT_PAY".equalsIgnoreCase(order.getStatus())) {
            message = "待支付,可取消";
        } else if ("PAYING".equalsIgnoreCase(order.getStatus())) {
            message = "支付中";
        } else {
            message = "订单处理中，不可取消";
        }
        return Result.fail(message, data);
    }

    @PostMapping("/orders/timeout-cancel")
    public Result<Map<String, Object>> timeoutCancel(
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
                        .in(OmsOrder::getStatus, "WAIT_PAY","PAY_FAILED","PAYING")
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
                            .in(OmsOrder::getStatus, "WAIT_PAY","PAY_FAILED","PAYING")
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

        return Result.ok(Map.of(
                "success", true,
                "minutes", minutes,
                "cutoff", cutoff.toString(),
                "scanned", candidates.size(),
                "updated", updated
        ));
    }

    @PostMapping("/orders/{orderNo}/pay")
    public Result<Map<String, Object>> pay(@PathVariable("orderNo") String orderNo,
                                          @RequestParam(value = "payNo", required = false) String payNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Result.fail("orderNo不能为空");
        }

        /*int rows = omsOrderMapper.update(
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
        }*/

        OmsOrder order = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );

        if (order == null) {
            return Result.fail("订单不存在");
        }

        if ("CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            return Result.ok("已支付", Map.of("orderNo", orderNo));
        }

        String st = order.getStatus();
        boolean canPay = "WAIT_PAY".equalsIgnoreCase(st) || "PAY_FAILED".equalsIgnoreCase(st);

        if (!canPay) {
            return Result.fail("当前状态不可支付:" + order.getStatus(), Map.of("orderNo", orderNo));
        }

        if (payNo == null || payNo.isBlank()) {
            payNo = "PAY-" + java.util.UUID.randomUUID();
        }

        orderPayLogMapper.insertIgnore(payNo, orderNo);

        String mappedOrderNo = orderPayLogMapper.selectOrderNoByPayNo(payNo);
        if (mappedOrderNo != null && !mappedOrderNo.equals(orderNo)) {
            return Result.fail("payNo已被其他订单使用", Map.of("orderNo", orderNo, "payNo", payNo));
        }

        int move = omsOrderMapper.update(
                null,
                new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .in(OmsOrder::getStatus, "WAIT_PAY", "PAY_FAILED")
                        .set(OmsOrder::getStatus, "PAYING")
                        .set(OmsOrder::getFailReason, null)
        );
        if (move == 0) {
            return Result.fail("当前状态不可支付:" + order.getStatus(), Map.of("orderNo", orderNo, "payNo", payNo));
        }

        /*int rows = omsOrderMapper.update(
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

        //rows==0 并发下 可能是被别的请求支付成功 按幂等处理
        OmsOrder latest = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (latest != null && "CONFIRMED".equalsIgnoreCase(latest.getStatus())) {
            resp.setSuccess(true);
            resp.setOrderNo(orderNo);
            resp.setMessage("已支付");
            return resp;
        }

        resp.setSuccess(false);
        resp.setOrderNo(orderNo);
        resp.setMessage("支付失败");
        return resp;*/

        OrderPayEvent payEvent = new OrderPayEvent();
        payEvent.setPayNo(payNo);
        payEvent.setOrderNo(orderNo);
        payEvent.setTs(System.currentTimeMillis());

        boolean sent = streamBridge.send("orderPayOut0", payEvent);
        if (!sent) {
            return Result.fail("支付受理失败:MQ发送失败", Map.of("orderNo", orderNo, "payNo", payNo));
        }

        return Result.ok("支付已受理:" + payNo, Map.of("orderNo", orderNo, "payNo", payNo));
    }

    @PostMapping("/pay/callback")
    public Result<Map<String, Object>> payCallback(@RequestParam("orderNo") String orderNo,
                                                  @RequestParam("payNo") String payNo,
                                                  @RequestParam(value = "delayMs", defaultValue = "0") long delayMs,
                                                  @RequestParam(value = "repeat", defaultValue = "1") int repeat,
                                                  @RequestParam(value = "result", defaultValue = "SUCCESS") String result) {
        if (orderNo == null || orderNo.isBlank() || payNo == null || payNo.isBlank()) {
            return Result.fail("orderNo/payNo不能为空");
        }

        if (result == null || result.isBlank()) {
            result = "SUCCESS";
        }
        result = result.toUpperCase();
        if (!"SUCCESS".equals(result) && !"FAIL".equals(result)) {
            result = "SUCCESS";
        }

        if (delayMs < 0) delayMs = 0;
        if (delayMs > 60000) delayMs = 60000;
        if (repeat <= 0) repeat = 1;
        if (repeat > 20) repeat = 20;

        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.fail("delay被中断", Map.of("orderNo", orderNo, "payNo", payNo));
            }
        }

        int sentCount = 0;
        for (int i = 0; i < repeat; i++) {
            OrderPayEvent payEvent = new OrderPayEvent();
            payEvent.setPayNo(payNo);
            payEvent.setOrderNo(orderNo);
            payEvent.setTs(System.currentTimeMillis());
            payEvent.setResult(result);

            boolean sent = streamBridge.send("orderPayOut0", payEvent);
            if (sent) {
                sentCount++;
            }
        }

        Map<String, Object> data = Map.of(
                "success", sentCount == repeat,
                "orderNo", orderNo,
                "payNo", payNo,
                "delayMs", delayMs,
                "repeat", repeat,
                "sent", sentCount,
                "result", result
        );
        if (sentCount != repeat) {
            return Result.fail("部分回调消息发送失败", data);
        }
        return Result.ok(data);
    }

    @PostMapping("/orders/{orderNo}/repay")
    public Result<Map<String, Object>> repay(@PathVariable("orderNo") String orderNo,
                                            @RequestParam(value = "result", defaultValue = "SUCCESS") String result,
                                            @RequestParam(value = "delayMs", defaultValue = "0") long delayMs,
                                            @RequestParam(value = "repeat", defaultValue = "1") int repeat) {
        if (orderNo == null || orderNo.isBlank()) {
            return Result.fail("orderNo不能为空");
        }

        if (result == null || result.isBlank()) {
            result = "SUCCESS";
        }
        result = result.toUpperCase();
        if (!"SUCCESS".equals(result) && !"FAIL".equals(result)) {
            result = "SUCCESS";
        }

        if (delayMs < 0) delayMs = 0;
        if (delayMs > 60000) delayMs = 60000;
        if (repeat <= 0) repeat = 1;
        if (repeat > 20) repeat = 20;

        OmsOrder order = omsOrderMapper.selectOne(
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (order == null) {
            return Result.fail("订单不存在");
        }

        String st = order.getStatus();
        boolean canPay = "WAIT_PAY".equalsIgnoreCase(st) || "PAY_FAILED".equalsIgnoreCase(st);
        if (!canPay) {
            return Result.fail("当前状态不可重新支付:" + st, Map.of("orderNo", orderNo));
        }

        String payNo = "PAY-" + java.util.UUID.randomUUID();

        orderPayLogMapper.insertIgnore(payNo, orderNo);

        String mappedOrderNo = orderPayLogMapper.selectOrderNoByPayNo(payNo);
        if (mappedOrderNo != null && !mappedOrderNo.equals(orderNo)) {
            return Result.fail("payNo已被其他订单使用", Map.of("orderNo", orderNo, "payNo", payNo));
        }

        int move = omsOrderMapper.update(
                null,
                new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getOrderNo, orderNo)
                        .in(OmsOrder::getStatus, "WAIT_PAY", "PAY_FAILED")
                        .set(OmsOrder::getStatus, "PAYING")
                        .set(OmsOrder::getFailReason, null)
        );
        if (move == 0) {
            return Result.fail("当前状态不可重新支付:" + st, Map.of("orderNo", orderNo, "payNo", payNo));
        }

        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.fail("delay被中断", Map.of("orderNo", orderNo, "payNo", payNo));
            }
        }

        int sentCount = 0;
        for (int i = 0; i < repeat; i++) {
            OrderPayEvent payEvent = new OrderPayEvent();
            payEvent.setPayNo(payNo);
            payEvent.setOrderNo(orderNo);
            payEvent.setResult(result);
            payEvent.setTs(System.currentTimeMillis());

            boolean sent = streamBridge.send("orderPayOut0", payEvent);
            if (sent) {
                sentCount++;
            }
        }

        Map<String, Object> data = Map.of(
                "success", sentCount == repeat,
                "orderNo", orderNo,
                "payNo", payNo,
                "result", result,
                "delayMs", delayMs,
                "repeat", repeat,
                "sent", sentCount,
                "message", "重新支付已受理"
        );
        if (sentCount != repeat) {
            return Result.fail("重新支付受理失败", data);
        }
        return Result.ok("重新支付已受理", data);
    }
}
