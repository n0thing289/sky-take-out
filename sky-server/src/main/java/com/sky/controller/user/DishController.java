package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.utils.CacheUtil;
import com.sky.utils.RedisUtil;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.function.Consumer;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Resource
    private DishService dishService;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("用户菜品浏览： categoryId={}", categoryId);

        //构造redis的key, dish_分类id
        String key = "dish_" + categoryId;
        //查询redis中是否存在菜品数据
        List<DishVO> list = CacheUtil.getCacheData(() -> {
            //如果存在直接返回
            return (List<DishVO>) redisUtil.get(key);
        }, () -> {
            //如果不存在查询数据库
            Dish dish = new Dish();
            dish.setCategoryId(categoryId);
            dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
            List<DishVO> resultList = dishService.listWithFlavor(dish);
            return resultList;
        }, o -> {
            redisUtil.set(key, o);
        });

        return Result.success(list);
    }

}
