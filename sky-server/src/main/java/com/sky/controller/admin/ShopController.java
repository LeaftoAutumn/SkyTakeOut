package com.sky.controller.admin;

import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 商户操作管理
 */
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "商户操作管理")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置商户营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result<String> setShopStatus(@PathVariable Integer status) {
        log.info("设置商户营业状态: {}", status);
        redisTemplate.opsForValue().set(RedisConstant.SHOP_STATUS, status);
        return Result.success();
    }

    /**
     * 获取商户营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result<Integer> getShopStatus() {
        log.info("商家获取商户营业状态");
        Integer status = (Integer) redisTemplate.opsForValue().get(RedisConstant.SHOP_STATUS);
        if (status == null) {
            // 默认营业状态
            redisTemplate.opsForValue().set(RedisConstant.SHOP_STATUS, StatusConstant.DISABLE);
            return Result.success(StatusConstant.DISABLE);
        }
        return Result.success(status);
    }
}
