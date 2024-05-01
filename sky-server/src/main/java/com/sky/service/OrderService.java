package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService extends IService<Orders> {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    OrderVO orderDetail(Long id);

    /**
     * 历史订单查询
     * @param ordersHistoryPageQueryDTO
     * @return
     */
    PageResult historyOrders(OrdersHistoryPageQueryDTO ordersHistoryPageQueryDTO);

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 客户催单
     * @param id
     */
    void reminder(Long id);

    /**
     * 用户取消订单
     * @param id
     */
    void cancel(Long id);

    /**
     * 用户再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 派送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);
}
