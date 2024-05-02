package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 营业额统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //查全部, 把begin(最小时间)和end(最大时间)的期间内要用的数据全部查出来
        List<Orders> orders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .select(Orders::getId, Orders::getAmount, Orders::getOrderTime)
                //查询date日期对于的营业额数据, 指的是状态为"已完成"的订单金额合计
                .eq(Orders::getStatus, Orders.COMPLETED)
                .between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
        //计算dateList
        List<LocalDate> dateList = this.getDateList(begin, end);
        String dateListStr = StringUtils.join(dateList, ",");//org.apache.commons.lang3
        //查询周期内每一天的金额
        List<Double> amountList = new ArrayList<>();
        dateList.forEach(date -> {
            //当天的最小时间, 例如 2024-04-25 0:0:0
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            //当天的最大时间, 例如 2024-05-01 23:59:59
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //过滤出当天的营业额
            Optional<BigDecimal> turnOverOp = orders.stream()
                    .filter(order -> order.getOrderTime().isAfter(beginTime) && order.getOrderTime().isBefore(endTime))
                    .map(Orders::getAmount)
                    .reduce(BigDecimal::add);
            Double turnOver = turnOverOp.map(BigDecimal::doubleValue).orElse(0.0);
            amountList.add(turnOver);
        });
        String amountListStr = StringUtils.join(amountList, ",");
        //封装数据vo
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(amountListStr)
                .build();
    }

    /**
     * 抽取出来的用于生成日期区间中每一天的list
     *
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        while (!begin.equals(end)) {//计算日期, 最后一天手动加进去
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        return dateList;
    }

    /**
     * 用户统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //查全部, 把begin(最小时间)和end(最大时间)的期间内要用的数据全部查出来
        List<User> users = userMapper.selectList(Wrappers.lambdaQuery(User.class)
                .select(User::getId, User::getCreateTime)
                .between(User::getCreateTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
        //计算dateList
        List<LocalDate> dateList = this.getDateList(begin, end);
        String dateListStr = StringUtils.join(dateList, ",");//org.apache.commons.lang3
        //新增用户数列表
        List<Long> newUserList = new ArrayList<>();
        //总用户量列表
        List<Long> totalUserList = new ArrayList<>();
        dateList.forEach(date -> {
            //当天的最小时间, 例如 2024-04-25 0:0:0
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            //当天的最大时间, 例如 2024-05-01 23:59:59
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //过滤出当天新增用户数
            long newUserCount = users.stream().filter(user -> user.getCreateTime().isAfter(beginTime) && user.getCreateTime().isBefore(endTime)).count();
            newUserList.add(newUserCount);
            //求出当天总用户量
            long totalUserCount = users.stream().filter(user -> user.getCreateTime().isBefore(endTime)).count();
            totalUserList.add(totalUserCount);
        });
        String newUserListStr = StringUtils.join(newUserList, ",");
        String totalUserListStr = StringUtils.join(totalUserList, ",");
        //封装数据
        return UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
    }

    /**
     * 订单统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        //查全部, 把begin(最小时间)和end(最大时间)的期间内要用的数据全部查出来
        List<Orders> orders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
        //计算dateList
        List<LocalDate> dateList = this.getDateList(begin, end);
        String dateListStr = StringUtils.join(dateList, ",");
        //获得每天订单数列表
        List<Long> orderCountList = new ArrayList<>();
        //获得每天有效订单数列表
        List<Long> validOrderCountList = new ArrayList<>();
        dateList.forEach(date -> {
            //当天的最小时间, 例如 2024-04-25 0:0:0
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            //当天的最大时间, 例如 2024-05-01 23:59:59
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //过滤出当天订单数
            long orderCount = orders.stream()
                    .filter(order -> order.getOrderTime().isAfter(beginTime) && order.getOrderTime().isBefore(endTime)).count();
            orderCountList.add(orderCount);
            //过滤出当天有效订单
            long validOrder = orders.stream()
                    .filter(order -> order.getOrderTime().isAfter(beginTime) && order.getOrderTime().isBefore(endTime))
                    .filter(o -> Orders.COMPLETED.equals(o.getStatus())).count();
            validOrderCountList.add(validOrder);
        });
        String orderCountListStr = StringUtils.join(orderCountList, ",");
        String validOrderCountListStr = StringUtils.join(validOrderCountList, ",");
        //统计订单总数
        Integer totalOrderCount = orders.size();
        //统计有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Long::sum).get().intValue();
        //计算订单完成率
        Double orderCompletionRate = ((double) validOrderCount / (double) totalOrderCount);
        //封装数据
        return OrderReportVO.builder()
                .dateList(dateListStr)//日期列表
                .orderCountList(orderCountListStr)//订单数列表
                .validOrderCountList(validOrderCountListStr)//有效订单数列表
                .totalOrderCount(totalOrderCount)//订单总数
                .validOrderCount(validOrderCount)//有效订单数
                .orderCompletionRate(orderCompletionRate)//订单完成率
                .build();
    }
}