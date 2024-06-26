package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {
    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void save(ShoppingCartDTO shoppingCartDTO);

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    void cleanSub(ShoppingCartDTO shoppingCartDTO);

}
