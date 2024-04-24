package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("套餐分页查询时需要的数据模型")
public class SetmealPageQueryDTO implements Serializable {

    @ApiModelProperty("当前页")
    private int page;
    @ApiModelProperty("每页条数")
    private int pageSize;
    @ApiModelProperty("套餐名")
    private String name;
    //分类id
    @ApiModelProperty("分类id")
    private Integer categoryId;

    //状态 0表示禁用 1表示启用
    @ApiModelProperty("状态 0表示禁用 1表示启用")
    private Integer status;

}
