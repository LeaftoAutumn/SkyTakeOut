package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时未支付订单
     */
    @Scheduled(cron = "0 * * * * *")
    public void processTimeOutOrder() {
        log.info("开始处理超时未支付订单");
        // 1.查询超时订单
        List<Orders> orders = orderMapper.queryOrdersByStatusAndDeadline(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if (orders != null && !orders.isEmpty()) {
            for (Orders order : orders) {
                // 2.更新订单状态
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("超时未支付，已自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理未收货订单
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void processTimeOutReceive() {
        log.info("开始处理未收货订单");
        // 1.查询超时订单
        List<Orders> orders = orderMapper.queryOrdersByStatusAndDeadline(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusHours(-2));
        if (orders != null && !orders.isEmpty()) {
            for (Orders order : orders) {
                // 2.更新订单状态
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }
}
