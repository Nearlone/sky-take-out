package com.sky.service.impl;

import com.aliyun.oss.common.utils.StringUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
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
        List<String> undeletableDishReasons = new ArrayList<>();

        for (Dish dish : dishes) {
            if (dish.getStatus() == StatusConstant.ENABLE) {
                undeletableDishReasons.add(MessageConstant.DISH_ON_SALE + ": " + dish.getName());
                continue;
            }
            Long setmealId = setmealDishMapper.findSetmealIdByDishId(dish.getId());
            if (setmealId != null){
                undeletableDishReasons.add(MessageConstant.DISH_BE_RELATED_BY_SETMEAL + ": " + dish.getName());
                continue;
            }
            deletableDishIds.add(dish.getId());
        }
        if (!deletableDishIds.isEmpty()) {
            dishMapper.deleteBatch(deletableDishIds);
            dishFlavorMapper.deleteByDishIds(deletableDishIds);
        }
        if (!undeletableDishReasons.isEmpty()) {
            throw new DeletionNotAllowedException(StringUtils.join("; \n", undeletableDishReasons));
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
}
