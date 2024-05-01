package com.sky.controller.user;

import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import com.sky.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Objects;

@RestController("userShopController")
@RequestMapping("/user")
@Slf4j
@Api(tags="C端-店铺相关接口")
public class ShopController {

    @Resource
    private RedisUtil redisUtil;

    /**
     * 用户端端查询营业状态
     * @return
     */
    @GetMapping("/shop/status")
    @ApiOperation("用户端端查询营业状态")
    Result<?> getShopStatusByUser(){
        Integer status = (Integer) redisUtil.get(RedisConstant.SHOP_STATUS);
        log.info("用户端端查询营业状态: {}", Objects.equals(status, StatusConstant.ENABLE) ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
