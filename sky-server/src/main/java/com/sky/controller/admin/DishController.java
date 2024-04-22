package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品相关接口")
public class DishController {
    @Resource
    private DishService dishService;

    /**
     * 菜品起售, 停售
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售, 停售")
    Result startOrStop(@PathVariable Integer status, Long id){
        log.info("菜品起售, 停售: status={},id={}", status,id);
        dishService.startOrStop(status, id);
        return Result.success();
    }



    /**
     * 根据分类id查询菜品
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    Result<List<Dish>> pageQuery(Long categoryId){
        log.info("根据分类id查询菜品: categoryId={}", categoryId);
        List<Dish> dishes = dishService.getDishByCategoryId(categoryId);
        return Result.success(dishes);
    }

    /**
     * 根据id查询菜品
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    Result<DishVO> getDish(@PathVariable Long id){
        log.info("根据id查询菜品: id={}", id);
        DishVO dishVO = dishService.getDish(id);
        return Result.success(dishVO);
    }

    /**
     * 菜品分页查询
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    Result<PageResult> pageQuery(DishPageQueryDTO dto){
        log.info("菜品分页查询: DishPageQueryDTO={}", dto);
        PageResult pageResult = dishService.pageQuery(dto);
        return Result.success(pageResult);
    }

    /**
     * 修改菜品
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    Result updateDish(@RequestBody DishDTO dto){
        log.info("修改菜品: dto={}", dto);
        dishService.update(dto);
        return Result.success();
    }

    /**
     * 批量删除菜品
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    Result deleteBatchDish(@RequestParam List<Long> ids){
        log.info("批量删除菜品: ids={}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 新增菜品
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    Result saveDish(@RequestBody DishDTO dto){
        log.info("新增菜品: dto={}", dto);
        dishService.saveWithFlavor(dto);
        return Result.success();
    }

}
