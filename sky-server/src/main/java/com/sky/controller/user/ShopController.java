package com.sky.controller.user;

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
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "商户操作管理")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取商户营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result<Integer> getShopStatus() {
        log.info("用户获取商户营业状态");
        Integer status = (Integer) redisTemplate.opsForValue().get(RedisConstant.SHOP_STATUS);
        if (status == null) {
            // 默认营业状态
            redisTemplate.opsForValue().set(RedisConstant.SHOP_STATUS, StatusConstant.DISABLE);
            return Result.success(StatusConstant.DISABLE);
        }
        return Result.success(status);
    }
}
