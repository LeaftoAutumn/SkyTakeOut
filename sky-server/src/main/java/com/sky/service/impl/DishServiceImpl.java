package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加菜品相关口味
     * @param flavors
     * @param dishId
     */
    private void addFlavor(List<DishFlavor> flavors, Long dishId) {
        if (flavors != null && !flavors.isEmpty()) {
            // 遍历口味列表，设置菜品id
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            // 批量插入口味数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 添加菜品和口味
     * @param dishDTO 菜品信息
     */
    @Transactional
    public void addDishAndFlavor(DishDTO dishDTO) {

        // 向菜品表中插入1条数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setStatus(StatusConstant.DISABLE); // 默认停售
        dishMapper.insert(dish);

        // 向口味表中插入n条数据
        addFlavor(dishDTO.getFlavors(), dish.getId());
    }

    /**
     * 菜品信息分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        // 查询菜品信息
        Page<DishVO> dishes = dishMapper.selectByPage(dishPageQueryDTO);

        return new PageResult(dishes.getTotal(), dishes.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    public void delete(List<Long> ids) {

        // 判断是否能被删除---> 1.是否在售
        for (Long id : ids) {
            Dish dish = dishMapper.selectByDishId(id);
            if (dish.getStatus().equals(StatusConstant.ENABLE)) {
                throw new RuntimeException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断是否能被删除---> 2.是否在套餐中
        Integer count = setmealDishMapper.countByDishIds(ids);
        if (count > 0) {
            throw new RuntimeException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品
        dishMapper.deleteBatch(ids);

        // 删除菜品口味
        dishFlavorMapper.deleteBatch(ids);
    }

    /**
     * 根据菜品id查询菜品
     * @param dishId
     * @return
     */
    public DishVO queryByDishId(Long dishId) {

        // 查询菜品
        Dish dish = dishMapper.selectByDishId(dishId);

        // 查询口味
        List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(dishId);

        // 封装数据
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);

        return dishVO;
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> queryByCategoryId(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Transactional
    public void updateDishAndFlavor(DishDTO dishDTO) {

        // 修改菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        // 删除口味
        dishFlavorMapper.deleteBatch(Collections.singletonList(dish.getId()));

        // 添加口味
        addFlavor(dishDTO.getFlavors(), dish.getId());
    }

    /**
     * 菜品起售、停售
     * @param dishId
     * @param status
     */
    public void dishStatusChange(Long dishId, Integer status) {
        // 若修改菜品为停售状态，修改关联的套餐状态为停售
        if (status.equals(StatusConstant.DISABLE)) {
            // 查询套餐id
            List<Long> ids = setmealDishMapper.getIdsByDishId(dishId);
            if (ids != null && !ids.isEmpty()) {
                // 修改套餐状态为停售
                ids.forEach(id -> {
                    Setmeal setmeal = setmealMapper.getById(id);
                    if (setmeal.getStatus().equals(StatusConstant.ENABLE)) {
                        setmeal.setStatus(StatusConstant.DISABLE);
                        setmealMapper.update(setmeal);
                    }
                });
            }
        }

        Dish dish = dishMapper.selectByDishId(dishId);
        dish.setStatus(status);
        dishMapper.update(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
