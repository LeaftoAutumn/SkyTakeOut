package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        
        shoppingCart = shoppingCartMapper.query(shoppingCart);

        // 购物车不存在，添加购物车
        if (shoppingCart == null) {
            shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

            // 判断待添加的是菜品还是套餐
            if (shoppingCart.getDishId() != null) {
                // 菜品
                Dish dish = dishMapper.selectByDishId(shoppingCart.getDishId());

                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            } else {
                // 套餐
                Setmeal setmeal = setmealMapper.getById(shoppingCart.getSetmealId());

                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            // 插入购物车
            shoppingCartMapper.insert(shoppingCart);
        } else {
            // 购物车已存在，数量加1
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.update(shoppingCart);
        }
    }

    /**
     * 查看购物车列表
     * @return
     */
    public List<ShoppingCart> list() {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 查询购物车列表
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.queryByUserId(userId);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            return Collections.emptyList();
        }

        // 返回购物车列表
        return shoppingCartList;
    }

    /**
     * 清空购物车
     */
    public void clean() {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 查询购物车
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);

        shoppingCart = shoppingCartMapper.query(shoppingCart);

        if (shoppingCart.getNumber() == 1) {
            // 删除购物车
            shoppingCartMapper.deleteById(shoppingCart.getId());
        } else {
            // 数量减1
            shoppingCart.setNumber(shoppingCart.getNumber() - 1);
            shoppingCartMapper.update(shoppingCart);
        }
    }
}
