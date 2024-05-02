package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    /**
     * 获取前10的销量列表
     * @param status
     * @param beginTime
     * @param endTime
     * @return
     */
    List<GoodsSalesDTO> getTop10(@Param("status") Integer status, @Param("beginTime") LocalDateTime beginTime, @Param("endTime") LocalDateTime endTime);
}
