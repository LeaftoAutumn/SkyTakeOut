package com.sky.service;

import com.sky.dto.DishDTO;

public interface DishService {

    /**
     * 添加菜品和口味
     * @param dishDTO 菜品信息
     */
    public void addDishAndFlavor(DishDTO dishDTO);
}
