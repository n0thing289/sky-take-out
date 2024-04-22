package com.sky.controller.admin;

import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import com.sky.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Objects;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags="店铺相关接口")
public class ShopController {

    @Resource
    private RedisUtil redisUtil;

    /**
     * 管理端查询营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("管理端查询营业状态")
    Result<Integer> getShopStatusByAdmin(){
        Integer status = (Integer) redisUtil.get(RedisConstant.SHOP_STATUS);
        log.info("管理端查询营业状态: {}", Objects.equals(status, StatusConstant.ENABLE) ? "营业中" : "打烊中");
        return Result.success(status);
    }

    /**
     * 设置营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    Result<?> setShopStatus(@PathVariable Integer status){
        log.info("设置店铺的营业状态为: {}", Objects.equals(status, StatusConstant.ENABLE) ? "营业中" : "打烊中");
        redisUtil.set(RedisConstant.SHOP_STATUS, status);
        return Result.success();
    }
}
