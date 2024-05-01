package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    @Resource
    private OrderMapper orderMapper;

    /**
     * 营业额统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
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
        for (LocalDate date : dateList) {
            //查询date日期对于的营业额数据, 指的是状态为"已完成"的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //
            Double turnOver = orderMapper.sumAmountByTime(Orders.COMPLETED, beginTime, endTime);
            turnOver = turnOver == null ? 0.0 : turnOver;
            amountList.add(turnOver);
        }
        String amountListStr = StringUtils.join(amountList, ",");
        //封装数据vo
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(amountListStr)
                .build();
    }
}
