package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController(value = "adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端-订单相关接口")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 接单
     * @param id
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody Long id) {
        log.info("接单: id={}", id);
        orderService.confirm(id);
        return Result.success();
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("管理端取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("管理端取消订单: id={}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics(){
        log.info("各个状态的订单数量统计: ");
        OrderStatisticsVO orderVO = orderService.statistics();
        return Result.success(orderVO);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id){
        log.info("查询订单详情: id={}",id);
        OrderVO orderVO = orderService.orderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单搜索: ordersPageQueryDTO={}",ordersPageQueryDTO);
        PageResult result = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(result);
    }
}
