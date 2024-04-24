package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Transactional(rollbackFor = SQLException.class)
public class SetmealServiceImpl implements SetmealService {

    @Resource
    private SetmealMapper setmealMapper;
    @Resource
    private SetmealDishMapper setmealDishMapper;
    @Resource
    private DishMapper dishMapper;

    /**
     * 条件查询
     *
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        //
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        //
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealPageQueryDTO, setmeal);
        List<Setmeal> setmeals = setmealMapper.selectList(Wrappers.lambdaQuery(Setmeal.class)
                .like(setmeal.getName() != null, Setmeal::getName, setmeal.getName())
                .eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus()));
        PageInfo<Setmeal> pageInfo = new PageInfo<>(setmeals);
        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Integer id) {
        //查出套餐
        Setmeal setmeal = setmealMapper.selectById(id);
        //查询套餐下的菜品
        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(Wrappers.lambdaQuery(SetmealDish.class)
                .eq(SetmealDish::getSetmealId, id));
        //封装数据
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishList);
        return setmealVO;
    }

    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //添加套餐
        setmealMapper.insert(setmeal);
        //添加套餐菜品
        setmealDTO.getSetmealDishes()
                .forEach(setmealDish -> {
                    //设置setmeal_id
                    setmealDish.setSetmealId(setmealDTO.getId());
                    setmealDishMapper.insert(setmealDish);
                });
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Override
    public void deleteBatch(String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        //删除套餐
        setmealMapper.deleteBatchIds(idList);
        //删除套餐菜品
        setmealDishMapper.delete(Wrappers.lambdaQuery(SetmealDish.class)
                .in(SetmealDish::getSetmealId, idList));
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //修改套餐
        setmealMapper.updateById(setmeal);
        //修改套餐菜品
        setmealDTO.getSetmealDishes()
                .forEach(setmealDish -> {
                    setmealDishMapper.update(setmealDish, Wrappers.lambdaUpdate(SetmealDish.class)
                            .eq(SetmealDish::getSetmealId, setmealDTO.getId()));
                });

    }

    /**
     * 套餐起售、停售
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //套餐起售、停售
        setmealMapper.update(Wrappers.lambdaUpdate(Setmeal.class)
                .set(Setmeal::getStatus, status)
                .eq(Setmeal::getId, id));
    }
}
