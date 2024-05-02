package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
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
                .select(Orders::getId)
                .select(Orders::getAmount)
                //查询date日期对于的营业额数据, 指的是状态为"已完成"的订单金额合计
                .eq(Orders::getStatus, Orders.COMPLETED)
                .between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
        //计算dateList
        List<LocalDate> dateList = new ArrayList<>();
        while (!begin.equals(end)) {//计算日期, 最后一天手动加进去
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
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
     * 用户统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //查全部, 把begin和end的期间内要用的数据全部查出来
        List<User> users = userMapper.selectList(Wrappers.lambdaQuery(User.class)
                .select(User::getId)
                .select(User::getCreateTime)
                .between(User::getCreateTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
        //计算dateList
        List<LocalDate> dateList = new ArrayList<>();
        while (!begin.equals(end)) {//计算日期, 最后一天手动加进去
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        String dateListStr = StringUtils.join(dateList, ",");//org.apache.commons.lang3
        //查询用户每天的新增用户
        //查询周期内总用户
        List<Long> newUserList = new ArrayList<>();
        List<Long> totalUserList = new ArrayList<>();
        dateList.forEach(date -> {
            //当天的最小时间, 例如 2024-04-25 0:0:0
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            //当天的最大时间, 例如 2024-05-01 23:59:59
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //过滤users在当天是新增
            long newUserCount = users.stream().filter(user -> user.getCreateTime().isAfter(beginTime) && user.getCreateTime().isBefore(endTime)).count();
            newUserList.add(newUserCount);
            //求当天总共的数量
            long allUserCount = users.stream().filter(user -> user.getCreateTime().isBefore(endTime)).count();
            totalUserList.add(allUserCount);
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
}