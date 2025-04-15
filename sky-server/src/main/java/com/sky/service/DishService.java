package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 添加菜品和口味
     * @param dishDTO 菜品信息
     */
    public void addDishAndFlavor(DishDTO dishDTO);

    /**
     * 菜品信息分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     * @param ids
     */
    void delete(List<Long> ids);

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    DishVO queryById(Long id);

    /**
     * 修改菜品
     * @param dishDTO
     */
    void updateDishAndFlavor(DishDTO dishDTO);

    /**
     * 菜品起售、停售
     * @param id
     * @param status
     */
    void dishStatusChange(Long id, Integer status);
}
