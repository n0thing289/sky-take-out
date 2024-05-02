package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
    /**
     * 插入订单数据
     * @param order
     * @return
     */
    int insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据状态, 开始时间, 结束时间, 来计算周期内总金额
     * @param status
     * @param beginTime
     * @param endTime
     */
    Double sumAmountByTime(@Param("status") Integer status, @Param("begin") LocalDateTime beginTime, @Param("end") LocalDateTime endTime);

    /**
     * 根据条件统计订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 根据条件统计营业额
     * @param map
     * @return
     */
    Double sumByMap(Map map);
}
