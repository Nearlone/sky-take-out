package com.sky.service.impl;

import com.aliyun.oss.common.utils.StringUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        // 获取生成的套餐id
        Long setmealId = setmeal.getId();
        // 批量插入套餐和菜品的关联关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
        }
        setmealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        List<SetmealVO> list = page.getResult();
        long total = page.getTotal();
        return new PageResult(total, list);
    }

    /**
     * 修改套餐 修改套餐要同时修改关联菜品
     *
     * @param setmealDTO
     */
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        setmealDishMapper.deleteBySetmealId(setmeal.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 根据id查询套餐信息
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO queryById(Long id) {
        SetmealVO setmealVO = setmealMapper.queryById(id);
        // 查询套餐下菜品
        setmealVO.setSetmealDishes(setmealDishMapper.getDishesBySetmealId(id));
        return setmealVO;
    }

    /**
     * 套餐起售、停售
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);
        setmeal.setId(id);
        setmealMapper.update(setmeal);
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // 起售状态的套餐不能删除，停售状态的套餐可以删除
        List<Long> deletableIds = new ArrayList<>();
        List<String> undeletableReasons = new ArrayList<>();
        for (Long id : ids) {
            SetmealVO setmeal = setmealMapper.queryById(id);
            if (setmeal.getStatus() == 1) {
                deletableIds.add(id);
            } else {
                undeletableReasons.add(MessageConstant.SETMEAL_ON_SALE + ": " + setmeal.getName());
            }
        }
        // 批量删除套餐 删除套餐时要同时删除套餐菜品关联数据
        if (deletableIds.size() > 0) {
            setmealMapper.deleteBatch(deletableIds);
            setmealDishMapper.deleteBySetmealIds(deletableIds);
        }
        if (undeletableReasons.size() > 0) {
            throw new DeletionNotAllowedException(StringUtils.join("; \n", undeletableReasons));
        }
    }
}
