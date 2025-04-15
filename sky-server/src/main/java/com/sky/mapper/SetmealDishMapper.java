package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询套餐的数量
     * @param ids
     * @return
     */
    Integer countByDishIds(List<Long> ids);
}
