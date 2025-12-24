package com.sky.service.impl;

import com.aliyun.oss.common.utils.StringUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品
     *
     * @param dishDTO
     */
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        dishDTO.getFlavors().forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
        dishFlavorMapper.insertBatch(dishDTO.getFlavors());
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        PageResult pageResult = new PageResult(page.getTotal(), page.getResult());
        return pageResult;
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        List<Dish> dishes = dishMapper.queryByIds(ids);

        List<Long> deletableDishIds = new ArrayList<>();
        List<String> undeletableReasons = new ArrayList<>();

        for (Dish dish : dishes) {
            if (dish.getStatus() == StatusConstant.ENABLE) {
                undeletableReasons.add(MessageConstant.DISH_ON_SALE + ": " + dish.getName());
                continue;
            }
            if (setmealDishMapper.countSetmealByDishId(dish.getId()) > 0){
                undeletableReasons.add(MessageConstant.DISH_BE_RELATED_BY_SETMEAL + ": " + dish.getName());
                continue;
            }
            deletableDishIds.add(dish.getId());
        }
        if (!deletableDishIds.isEmpty()) {
            dishMapper.deleteBatch(deletableDishIds);
            dishFlavorMapper.deleteByDishIds(deletableDishIds);
        }
        if (!undeletableReasons.isEmpty()) {
            throw new DeletionNotAllowedException(StringUtils.join("; \n", undeletableReasons));
        }

    }

    /**
     * 根据id查询菜品和对应的口味数据
     *
     * @param id
     * @return
     */
    @Override
    public DishVO queryById(Long id) {
        DishVO dishVO = new DishVO();

        Dish dish = dishMapper.queryById(id);
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavorMapper.queryByDishId(id));
        return dishVO;
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     */
    @Override
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.update(dish);

        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        dishDTO.getFlavors().forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
        dishFlavorMapper.insertBatch(dishDTO.getFlavors());
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 菜品如果有关联套餐，则不能停售
        if (setmealDishMapper.countSetmealByDishId(id) > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_DISABLE_FAILED);
        }
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @Override
    public List<DishVO> listByCategory(Long categoryId) {
        List<DishVO> dishList = dishMapper.listByCategory(categoryId);
        return dishList;
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<DishVO> dishList = dishMapper.listByCategory(dish.getCategoryId());

        for (DishVO d : dishList) {

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.queryByDishId(d.getId());

            d.setFlavors(flavors);
        }

        return dishList;
    }
}
