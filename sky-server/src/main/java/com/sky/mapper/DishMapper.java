package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     *
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品数据
     *
     * @param dish
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into dish (name, category_id, price, image, status, description, create_time, update_time, create_user, update_user) " +
            "values (#{name}, #{categoryId}, #{price}, #{image}, #{status}, #{description}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Dish dish);

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据id查询菜品和对应的口味数据
     *
     * @param ids
     * @return
     */
    List<Dish> queryByIds(List<Long> ids);

    /**
     * 批量删除菜品
     *
     * @param deletableDishIds
     */
    void deleteBatch(List<Long> deletableDishIds);

    /**
     * 根据id查询菜品数据
     *
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Dish queryById(Long id);

    /**
     * 修改菜品数据
     *
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @Select("select * from dish where category_id = #{categoryId} and status = 1")
    List<DishVO> listByCategory(Long categoryId);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
