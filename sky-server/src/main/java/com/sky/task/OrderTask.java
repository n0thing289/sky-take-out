package com.sky.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Resource
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     * 每分钟检查一次是否存在支付超时订单, 如果超时, 修改订单为已取消
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeOutOrders() {
        log.info("定时处理超时订单: {}", LocalDateTime.now());
        List<Orders> timeOutOrders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.PENDING_PAYMENT)
                .lt(Orders::getOrderTime, LocalDateTime.now().minusMinutes(15)));
        if (!ObjectUtils.isEmpty(timeOutOrders)){
            timeOutOrders.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时, 自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.updateById(order);
            });
        }

    }

    /**
     * 处理一直派送订单
     * 每天凌晨一点检查一次是否存在支付超时订单, 如果超时, 修改订单为已取消
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrders() {
        log.info("定时处理一直派送订单: {}", LocalDateTime.now());
        List<Orders> deliveringOrders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getOrderTime, LocalDateTime.now().minusHours(1)));
        if (!ObjectUtils.isEmpty(deliveringOrders)){
            deliveringOrders.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.updateById(order);
            });
        }
    }
}
