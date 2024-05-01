package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;

@ApiModel("历史订单查询时需要传递的数据模型")
@Data
public class OrdersHistoryPageQueryDTO {
    @ApiModelProperty("状态")
    private Integer status;

    //页码
    @ApiModelProperty("页码")
    private int page;

    //每页显示记录数
    @ApiModelProperty("每页显示记录数")
    private int pageSize;
}
