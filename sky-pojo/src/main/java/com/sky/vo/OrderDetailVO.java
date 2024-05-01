package com.sky.vo;

import com.sky.entity.OrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class OrderDetailVO {
    //地址
    private String address;
    //地址簿id
    private Long addressBookid;
    //金额
    private Double amount;
    //取消原因
    private String cancelReason;
    //取消时间
    private LocalDateTime cancelTime;
    //超时时间
    private LocalDateTime checkoutTime;
    //收货人
    private String consignee;
    //配送状态
    private Long deliveryStatus;
    //配送时间
    private LocalDateTime deliveryTime;
    //预计配送时间
    private LocalDateTime estimatedDeliveryTime;
    //id
    private Long id;
    //订单号
    private String number;
    //订单明细
    private List<OrderDetail> orderDetailList;
    //下单时间
    private LocalDateTime orderTime;
    //打包费
    private Long packAmount;
    //支付方式
    private Long payMethod;
    //支付状态
    private Long payStatus;
    //手机号
    private String phone;
    //拒单原因
    private String rejectionReason;
    //备注
    private String remark;
    //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款
    private Long status;
    //餐具数量
    private Long tablewareNumber;
    //餐具数量状态  1按餐量提供  0选择具体数量
    private Long tablewareStatus;
    //用户id
    private Long userid;
    //用户名
    private String userName;
}
