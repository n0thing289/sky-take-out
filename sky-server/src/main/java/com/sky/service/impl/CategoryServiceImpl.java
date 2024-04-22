package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private DishMapper dishMapper;

    @Resource
    private SetmealMapper setmealMapper;

    /**
     * 根据id删除
     * @param id
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void removeById(Long id) {
        //查询当前分类是否关联了菜品, 如果关联就抛出异常
        int count = dishMapper.countByCategoryId(id);
        if (count>0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        //查询当前分类是否关联了套餐, 如果关联就抛出异常
        count = setmealMapper.countByCategoryId(id);
        if (count>0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        //删除
        categoryMapper.deleteById(id);
    }

    /**
     * 分类分页查询
     *
     * @param categoryPageQueryDTO
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        //获取分页信息
        int current = categoryPageQueryDTO.getPage();
        int pageSize = categoryPageQueryDTO.getPageSize();
        //创建查询条件
        String name = categoryPageQueryDTO.getName();
        Integer type = categoryPageQueryDTO.getType();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, Category::getName, name)
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSort);
        //开始分页查询
        PageHelper.startPage(current, pageSize);
        List<Category> categoryList = categoryMapper.selectList(wrapper);
        //封装查询结果
        PageInfo<Category> pageInfo = new PageInfo<>(categoryList);
        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 启用,禁用分类
     *
     * @param status
     * @param id
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void startOrStop(Integer status, Long id) {
        //创建修改条件
        //update category set status=#{status},update_time=#{},update_user=#{} where id = #{}
        LambdaUpdateWrapper<Category> wrapper = new LambdaUpdateWrapper<Category>()
                .set(Category::getStatus, status)
                .set(Category::getUpdateTime, LocalDateTime.now())
                .set(Category::getUpdateUser, BaseContext.getCurrentId())
                .eq(Category::getId, id);//id必填
        //更新表
        categoryMapper.update(wrapper);
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public List<Category> listByType(Integer type) {
        //查询条件
        LambdaQueryWrapper<Category> wrapper = Wrappers.lambdaQuery(Category.class)
                .eq(Category::getType, type)
                .orderByAsc(Category::getSort);
        //查询
        return categoryMapper.selectList(wrapper);
    }

    /**
     * 新增分类
     * @param categoryDTO
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void save(CategoryDTO categoryDTO) {
        //对象复制
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        //默认新套餐分类是禁用状态
        category.setStatus(StatusConstant.DISABLE);
        //设置当前记录的创建时间和修改时间
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        //设置当前记录创建人和修改人id
        //当前登录用户id
        Long currentId = BaseContext.getCurrentId();
        category.setCreateUser(currentId);
        category.setUpdateUser(currentId);
        //插入表
        categoryMapper.insert(category);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void update(CategoryDTO categoryDTO) {
        //对象复制
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        //设置修改时间和设置当前记录修改人id
        category.setUpdateTime(LocalDateTime.now());
        category.setUpdateUser(BaseContext.getCurrentId());
        //更新表
        categoryMapper.updateById(category);
    }
}
