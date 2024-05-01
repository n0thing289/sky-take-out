package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("营业额统计接口时需要的数据")
public class DataOverViewQueryDTO implements Serializable {

    //起始日期
    @ApiModelProperty("起始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime begin;
    //结束日期
    @ApiModelProperty("结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime end;

}
