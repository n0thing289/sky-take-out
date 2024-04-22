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
@RequestMapping("/user/shop")
@Slf4j
@Api(tags="店铺相关接口")
public class ShopController {

    @Resource
    private RedisUtil redisUtil;

    /**
     * 用户端端查询营业状态
     * @return
     */
    @GetMapping
    @ApiOperation("管理端查询营业状态")
    Result<?> getShopStatusByUser(){

        return Result.success();
    }
}
