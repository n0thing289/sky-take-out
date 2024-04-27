package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.*;

@Service
@Transactional
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Resource
    private DishMapper dishMapper;

    @Resource
    private DishFlavorMapper dishFlavorMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品
     *
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void saveWithFlavor(DishDTO dto) {
        //对象复制
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);
        //添加dish
        dish.setStatus(StatusConstant.DISABLE);//第一次默认禁用
        dishMapper.insert(dish);
        //添加口味
        List<DishFlavor> dishFlavors = dto.getFlavors();
        if (!ObjectUtils.isEmpty(dishFlavors)) {
            //TODO note要想获取Mybatis Plus的insert（）执行后的自增长id其实很简单，你不需要多做任何的操作，你只需要在它执行之后，拿你传进去的对象点它的id属性即可
            dishFlavors.parallelStream()
                    .forEachOrdered((dishFlavor -> {
                        //设置dish_id
                        dishFlavor.setDishId(dish.getId());
                        dishFlavorMapper.insert(dishFlavor);
                    }));
        }

    }

    /**
     * 批量删除菜品
     *
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void deleteBatch(List<Long> ids) {
        //获得起售状态的dishId  --判断是否起售, 起售状态不能删
        List<Dish> dishList = dishMapper.selectList(Wrappers.lambdaQuery(Dish.class)
                .select(Dish::getId)
                .select(Dish::getStatus)
                .in(Dish::getId, ids));
        for (Dish dish : dishList) {
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                ids.remove(dish.getId());
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //获得根据判断dish_id列中有无在dishIds中的 --判断是否套餐关联, 有套餐关联的不能删
        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(Wrappers.lambdaQuery(SetmealDish.class)
                .select(SetmealDish::getDishId)
                .select(SetmealDish::getName)
                .in(SetmealDish::getDishId, ids));
        for (SetmealDish setmealDish : setmealDishList) {
            if (ids.contains(setmealDish.getDishId())) {
                ids.remove(setmealDish.getDishId());
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        }
        //批量删除菜品
        dishMapper.deleteBatchIds(ids);
        //批量删除对应的口味
        dishFlavorMapper.delete(Wrappers.lambdaQuery(DishFlavor.class)
                .in(DishFlavor::getDishId, ids));
    }

    /**
     * 修改菜品
     *
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void update(DishDTO dto) {
        //对象赋值
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);
        //修改菜品
        dishMapper.updateById(dish);//UPDATE dish SET name=?, category_id=?, price=?, image=?, description=?, status=?, update_time=?, update_user=? WHERE id=?
        //修改此菜品的口味 -> 删除原有的口味数据, 重新插入口味数据
        //  删除原有的口味数据
        dishFlavorMapper.delete(Wrappers.lambdaQuery(DishFlavor.class)
                .eq(DishFlavor::getDishId, dish.getId()));
        //  重新插入口味数据
        List<DishFlavor> flavors = dto.getFlavors();
        for (DishFlavor flavor:flavors){
            flavor.setDishId(dish.getId());
            dishFlavorMapper.insert(flavor);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult pageQuery(DishPageQueryDTO dto) {
        //条件
        Integer categoryId = dto.getCategoryId();
        String name = dto.getName();
        Integer status = dto.getStatus();
        LambdaQueryWrapper<Dish> wrapper = Wrappers.lambdaQuery(Dish.class)
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .like(name != null, Dish::getName, name)
                .eq(status != null, Dish::getStatus, status)
                .orderByDesc(Dish::getCreateTime);
        //分页
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        //查询
        List<Dish> dishes = dishMapper.selectList(wrapper);
        List<Category> categories = categoryMapper.selectList(null);
        List<DishVO> dishVOS = new ArrayList<>();
        dishes.parallelStream()
                .forEachOrdered(dish -> {
                    DishVO dishVo = new DishVO();
                    BeanUtils.copyProperties(dish, dishVo);
                    //设置分类名称
                    Long dish_categoryId = dish.getCategoryId();
                    categories.parallelStream()
                            .filter(item -> Objects.equals(item.getId(), dish_categoryId))
                            .findAny()
                            .ifPresent(category -> {
                                dishVo.setCategoryName(category.getName());
                            });
                    //添加给vo
                    dishVOS.add(dishVo);
                });
        PageInfo<Dish> info = new PageInfo<>(dishes);
        return new PageResult(info.getTotal(), dishVOS);
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public DishVO getDish(Long id) {
        //查询菜品
        Dish dish = dishMapper.selectById(id);
        //查询菜品的口味
        LambdaQueryWrapper<DishFlavor> wrapper = Wrappers.lambdaQuery(DishFlavor.class)
                .eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(wrapper);
        //对象复制
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public List<Dish> getDishByCategoryId(Long categoryId) {
        //查询菜品
        List<Dish> dishes = dishMapper.selectList(Wrappers.lambdaQuery(Dish.class)
                .eq(Dish::getCategoryId, categoryId));
        return dishes;
    }

    /**
     * 菜品起售, 停售
     *
     * @param status
     * @param id
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        LambdaUpdateWrapper<Dish> wrapper = Wrappers.lambdaUpdate(dish)
                .set(Dish::getStatus, status)
                .eq(Dish::getId, id);
        dishMapper.update(dish, wrapper);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.selectList(Wrappers.lambdaQuery(dish));//TODO 可能不对
//        .eq(Dish::getCategoryId, dish.getCategoryId())
//                .eq(Dish::getStatus, dish.getStatus())

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectList(Wrappers.lambdaQuery(DishFlavor.class)
                    .eq(DishFlavor::getDishId, d.getId()));

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
