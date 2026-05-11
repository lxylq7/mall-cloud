package com.lxylq7.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("oms_order")
public class OmsOrder {
    @TableId(type = IdType.AUTO)  //可以删掉 局部优先
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private String status;
    private LocalDateTime createAt;
    private String failReason;

    public OmsOrder() {
    }

    public OmsOrder(Long id, String orderNo, Long userId, Long productId, Integer quantity, String status, LocalDateTime createAt, String failReason) {
        this.id = id;
        this.orderNo = orderNo;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createAt = createAt;
        this.failReason = failReason;
    }

    /**
     * 获取
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取
     * @return orderNo
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 设置
     * @param orderNo
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取
     * @return userId
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置
     * @param userId
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取
     * @return productId
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 设置
     * @param productId
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 获取
     * @return quantity
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 设置
     * @param quantity
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 获取
     * @return status
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置
     * @param status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取
     * @return createAt
     */
    public LocalDateTime getCreateAt() {
        return createAt;
    }

    /**
     * 设置
     * @param createAt
     */
    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    /**
     * 获取
     * @return failReason
     */
    public String getFailReason() {
        return failReason;
    }

    /**
     * 设置
     * @param failReason
     */
    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String toString() {
        return "OmsOrder{id = " + id + ", orderNo = " + orderNo + ", userId = " + userId + ", productId = " + productId + ", quantity = " + quantity + ", status = " + status + ", createAt = " + createAt + ", failReason = " + failReason + "}";
    }
}