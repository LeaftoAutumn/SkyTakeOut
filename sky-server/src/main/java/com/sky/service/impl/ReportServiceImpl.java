package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取营业额统计数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        List<Double> turnoverList = new ArrayList<>();

        LocalDate currentDate = LocalDate.now();
        for (LocalDate date : dateList) {
            // 如果日期大于当前日期，则营业额为0
            if (date.isAfter(currentDate)) {
                turnoverList.add(0.0);
                continue;
            }
            // 如果日期小于当前日期，则从尝试从缓存中获取营业额
            // 获取缓存的key
            String key = "turnover_" + date.toString();
            if (!date.equals(currentDate)) {
                // 从缓存中获取数据
                Double redisTurnover = (Double) redisTemplate.opsForValue().get(key);
                if (redisTurnover != null) {
                    turnoverList.add(redisTurnover);
                    continue;
                }
            }

            LocalDateTime startTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Map<String, Object> map = new HashMap<>();
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.getTurnoverByDate(map);
            if (turnover == null) {
                turnover = 0.0;
            }

            if (!date.equals(currentDate)) {
                redisTemplate.opsForValue().set(key, turnover);
            }
            turnoverList.add(turnover);
        }

        // 将日期和营业额转换为字符串
        return new TurnoverReportVO()
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 获取用户统计数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        LocalDate currentDate = LocalDate.now();
        for (LocalDate date : dateList) {
            // 如果日期大于当前日期，则用户总量和新增数量为0
            if (date.isAfter(currentDate)) {
                totalUserList.add(0);
                newUserList.add(0);
                continue;
            }
            // 如果日期小于当前日期，则从尝试从缓存中获取用户总量和新增数量
            if (!date.equals(currentDate)) {
                // 获取缓存的key
                String key = "totalUser_" + date.toString();
                // 从缓存中获取数据
                Integer redisTotalUser = (Integer) redisTemplate.opsForValue().get(key);
                if (redisTotalUser != null) {
                    totalUserList.add(redisTotalUser);
                }
                key = "newUser_" + date.toString();
                Integer redisNewUser = (Integer) redisTemplate.opsForValue().get(key);
                if (redisNewUser != null) {
                    newUserList.add(redisNewUser);
                    continue;
                }
            }

            LocalDateTime startTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Map<String, Object> map = new HashMap<>();
            map.put("startTime", startTime);
            map.put("endTime", endTime);

            // 获取用户总量
            Integer totalUser = orderMapper.getTotalUserByDate(map);
            // 获取新增用户
            Integer newUser = userMapper.getNewUserByDate(map);

            if (!date.equals(currentDate)) {
                String key = "totalUser_" + date.toString();
                redisTemplate.opsForValue().set(key, totalUser);
                key = "newUser_" + date.toString();
                redisTemplate.opsForValue().set(key, newUser);
            }
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        // 将日期、用户总量和新增用户转换为字符串
        return new UserReportVO()
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }
}
