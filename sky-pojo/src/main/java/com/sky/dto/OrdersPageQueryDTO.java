package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("订单搜索时需要传递的数据模型")
public class OrdersPageQueryDTO implements Serializable {

    //当前页
    @ApiModelProperty("当前页")
    private int page;
    //每页条数
    @ApiModelProperty("每页条数")
    private int pageSize;
    //订单号
    @ApiModelProperty("订单号")
    private String number;
    //手机号
    @ApiModelProperty("手机号")
    private String phone;
    //订单状态
    @ApiModelProperty("订单状态")
    private Integer status;
    //下单时间
    @ApiModelProperty("下单时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;
    //完成时间
    @ApiModelProperty("完成时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    //用户id
    @ApiModelProperty("用户id")
    private Long userId;

}
