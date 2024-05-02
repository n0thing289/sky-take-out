package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private OrderDetailMapper orderDetailMapper;

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
        List<Orders> orders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class).select(Orders::getAmount, Orders::getOrderTime)
                //查询date日期对于的营业额数据, 指的是状态为"已完成"的订单金额合计
                .eq(Orders::getStatus, Orders.COMPLETED).between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
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
            Optional<BigDecimal> turnOverOp = orders.stream().filter(order -> order.getOrderTime().isAfter(beginTime) && order.getOrderTime().isBefore(endTime)).map(Orders::getAmount).reduce(BigDecimal::add);
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
        List<User> users = userMapper.selectList(Wrappers.lambdaQuery(User.class).select(User::getCreateTime).between(User::getCreateTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
        //计算dateList
        List<LocalDate> dateList = this.getDateList(begin, end);
//        String dateListStr = StringUtils.join(dateList, ",");//org.apache.commons.lang3
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
        //封装数据
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
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
        List<Orders> orders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class).select(Orders::getOrderTime, Orders::getStatus).between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));
        //计算dateList
        List<LocalDate> dateList = this.getDateList(begin, end);
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
            long orderCount = orders.stream().filter(order -> order.getOrderTime().isAfter(beginTime) && order.getOrderTime().isBefore(endTime)).count();
            orderCountList.add(orderCount);
            //过滤出当天有效订单
            long validOrder = orders.stream().filter(order -> order.getOrderTime().isAfter(beginTime) && order.getOrderTime().isBefore(endTime)).filter(o -> Orders.COMPLETED.equals(o.getStatus())).count();
            validOrderCountList.add(validOrder);
        });
        //统计订单总数
        Integer totalOrderCount = orders.size();
        //统计有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Long::sum).get().intValue();
        //计算订单完成率
        Double orderCompletionRate = ((double) validOrderCount / (double) totalOrderCount);
        //封装数据
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))//日期列表
                .orderCountList(StringUtils.join(orderCountList, ","))//订单数列表
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))//有效订单数列表
                .totalOrderCount(totalOrderCount)//订单总数
                .validOrderCount(validOrderCount)//有效订单数
                .orderCompletionRate(orderCompletionRate)//订单完成率
                .build();
    }

    /**
     * 查询销量排名top10接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Deprecated
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        //查出orders的id列表, 把begin(最小时间)和end(最大时间)的期间内要用的数据全部查出来
        List<Long> orderIdList = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class).select(Orders::getId).between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)).eq(Orders::getStatus, Orders.COMPLETED)).stream().map(Orders::getId).collect(Collectors.toList());
        //查询当期订单明细, 根据出现的当期订单id
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(Wrappers.lambdaQuery(OrderDetail.class).select(OrderDetail::getName, OrderDetail::getDishId, OrderDetail::getSetmealId, OrderDetail::getNumber).in(OrderDetail::getOrderId, orderIdList));
        List<OrderDetail> orderDetails2 = new ArrayList<>(orderDetails);
        Set<Long> dishIdedList = new HashSet<>();//已经出现过的菜品id
        Set<Long> setmealIdedList = new HashSet<>();//已经出现过的套餐id
        //销量分菜品和套餐, 菜品/套餐在订单明细出现的次数与份数的积,作为销量标准
        //菜品套餐销量map
        HashMap<String, Long> map = new HashMap<>();
        orderDetails.forEach(orderDetail -> {
            Long dishId = orderDetail.getDishId();
            if (dishId != null && !dishIdedList.contains(dishId)) {//统计某菜品的销量
                //过滤出当前的菜品id出现次数
                long dishIdShowCount = orderDetails2.stream().filter(od -> dishId.equals(od.getDishId())).count();
                //将当前菜品名字和 菜品id出现次数与份数的积(销量) 放入map
                map.put(orderDetail.getName(), dishIdShowCount * orderDetail.getNumber());
                //删去列表中所有含当前菜品id的元素
                orderDetails2.removeIf(od -> dishId.equals(od.getDishId()));
                //将出现的菜品id存入dishIdedList
                dishIdedList.add(dishId);
            } else {//统计套餐销量
                Long setmealId = orderDetail.getSetmealId();
                if (setmealId != null && !setmealIdedList.contains(setmealId)) {
                    //过滤出当前的套餐id出现次数
                    long setmealIdShowCount = orderDetails2.stream().filter(od -> setmealId.equals(od.getSetmealId())).count();
                    //将当前套餐名字和 套餐id出现次数与份数的积(销量) 放入map
                    map.put(orderDetail.getName(), setmealIdShowCount * orderDetail.getNumber());
                    //删去列表中所有含当前套餐id的元素
                    orderDetails2.removeIf(od -> setmealId.equals(od.getSetmealId()));
                    //将出现的套餐id存入setmealIdedList
                    setmealIdedList.add(setmealId);
                }
            }
        });
        //商品名称列表
        List<String> nameList = new ArrayList<>();
        //销量列表
        List<Long> numberList = new ArrayList<>();
        map.entrySet().stream().sorted((o1, o2) -> (int) (o2.getValue() - o1.getValue()))//降序
                .forEach(entry -> {
                    nameList.add(entry.getKey());
                    numberList.add(entry.getValue());
                });
        String nameListStr = StringUtils.join(nameList, ",");
        String numberListStr = StringUtils.join(numberList, ",");
        //封装数据
        return SalesTop10ReportVO.builder().nameList(nameListStr).numberList(numberListStr).build();
    }

    /**
     * 查询销量排名top10接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10New(LocalDate begin, LocalDate end) {
        //获取销量前十的
        List<GoodsSalesDTO> top10 = orderDetailMapper.getTop10(null, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        //商品名称列表
        List<String> nameList = new ArrayList<>();
        //销量列表
        List<Integer> numberList = new ArrayList<>();
        top10.forEach(goods -> {
            nameList.add(goods.getName());
            numberList.add(goods.getNumber());
        });
        //封装数据
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }
}