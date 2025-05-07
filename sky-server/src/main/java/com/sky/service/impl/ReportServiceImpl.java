package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
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
    private WorkspaceService workspaceService;
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
                redisTemplate.opsForValue().set(key, turnover, Duration.ofDays(1));
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
                redisTemplate.opsForValue().set(key, totalUser, Duration.ofDays(1));
                key = "newUser_" + date.toString();
                redisTemplate.opsForValue().set(key, newUser, Duration.ofDays(1));
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

    /**
     * 获取订单统计数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer totalValidOrderCount = 0;

        LocalDate currentDate = LocalDate.now();
        for (LocalDate date : dateList) {
            // 如果日期大于当前日期，则订单总数和有效订单数为0
            if (date.isAfter(currentDate)) {
                orderCountList.add(0);
                validOrderCountList.add(0);
                continue;
            }
            // 如果日期小于当前日期，则从尝试从缓存中获取订单总数和有效订单数
            if (!date.equals(currentDate)) {
                // 获取缓存的key
                String key = "orderCount_" + date.toString();
                // 从缓存中获取数据
                Integer redisOrderCount = (Integer) redisTemplate.opsForValue().get(key);
                if (redisOrderCount != null) {
                    orderCountList.add(redisOrderCount);
                    totalOrderCount += redisOrderCount;
                }
                key = "validOrderCount_" + date.toString();
                Integer redisValidOrderCount = (Integer) redisTemplate.opsForValue().get(key);
                if (redisValidOrderCount != null) {
                    validOrderCountList.add(redisValidOrderCount);
                    totalValidOrderCount += redisValidOrderCount;
                    continue;
                }
            }

            LocalDateTime startTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Map<String, Object> map = new HashMap<>();
            map.put("startTime", startTime);
            map.put("endTime", endTime);

            // 获取订单总数
            Integer orderCount = orderMapper.getOrderCountByDate(map);
            totalOrderCount += orderCount;
            // 获取有效订单数
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.getOrderCountByDate(map);
            totalValidOrderCount += validOrderCount;

            if (!date.equals(currentDate)) {
                String key = "orderCount_" + date.toString();
                redisTemplate.opsForValue().set(key, orderCount, Duration.ofDays(1));
                key = "validOrderCount_" + date.toString();
                redisTemplate.opsForValue().set(key, validOrderCount, Duration.ofDays(1));
            }
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        return new OrderReportVO()
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCompletionRate(totalOrderCount == 0 ? 1 : totalValidOrderCount / totalOrderCount.doubleValue())
                .build();
    }

    /**
     * 获取销量前十统计数据
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10Statistics(LocalDate begin, LocalDate end) {
        List<String> productNameList = new ArrayList<>();
        List<Integer> salesCountList = new ArrayList<>();

        // 更新结束时间为当前时间，后续无订单
        end = end.isAfter(LocalDate.now()) ? LocalDate.now() : end;

        String key = "salesTop10_" + begin.toString() + "_" + end.toString();
        // 从缓存中获取数据
        List<GoodsSalesDTO> redisSalesTop10 = (List<GoodsSalesDTO>) redisTemplate.opsForValue().get(key);
        if (redisSalesTop10 != null) {
            for (GoodsSalesDTO map : redisSalesTop10) {
                productNameList.add(map.getName());
                salesCountList.add(map.getNumber());
            }
            return new SalesTop10ReportVO()
                    .builder()
                    .nameList(StringUtils.join(productNameList, ","))
                    .numberList(StringUtils.join(salesCountList, ","))
                    .build();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("startTime", begin.atStartOfDay());
        map.put("endTime", end.plusDays(1).atStartOfDay());
        map.put("status", Orders.COMPLETED);

        // 获取销量前十
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10ByDate(map);
        for (GoodsSalesDTO sales : salesTop10) {
            productNameList.add(sales.getName());
            salesCountList.add(sales.getNumber());
        }
        // 将销量前十存入缓存
        redisTemplate.opsForValue().set(key, salesTop10, Duration.ofMinutes(1));

        return new SalesTop10ReportVO()
                .builder()
                .nameList(StringUtils.join(productNameList, ","))
                .numberList(StringUtils.join(salesCountList, ","))
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

    /**
     * 导出运营数据报表
     * @param response
     */
    public void export(HttpServletResponse response) {
        // 1、获取起止时间
        // 1.1、获取当前时间
        LocalDate currentDate = LocalDate.now();
        // 1.2、获取30天前的时间
        LocalDate begin = currentDate.minusDays(30);

        // 2、通过POI将数据写入Excel文件
        // 2.1、获取Excel模板
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/Operational data report.xlsx");
        try {
            // 创建POI对象
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            // 获取模板所在页sheet
            XSSFSheet sheet = xssfWorkbook.getSheetAt(0);

            // 设置sheet名称
            xssfWorkbook.setSheetName(0, "运营数据报表");

            // 设置报表统计的时间区间
            sheet.getRow(1).getCell(1).setCellValue("统计时间区间" + begin + "至" + currentDate);

            // 获取近30天的营业数据统计（概览数据）
            BusinessDataVO businessData = workspaceService.getBusinessData(begin.atStartOfDay(), currentDate.plusDays(1).atStartOfDay());
            // 设置概览数据
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            // 获取近30天每天的营业数据统计（明细数据）
            for (int i = 0; i < 30; i++) {
                // 获取每天的营业数据统计
                businessData = workspaceService.getBusinessData(currentDate.atStartOfDay(), currentDate.plusDays(1).atStartOfDay());
                // 设置每天的营业数据统计
                sheet.getRow(i + 7).getCell(1).setCellValue(currentDate.toString());
                sheet.getRow(i + 7).getCell(2).setCellValue(businessData.getTurnover());
                sheet.getRow(i + 7).getCell(3).setCellValue(businessData.getValidOrderCount());
                sheet.getRow(i + 7).getCell(4).setCellValue(businessData.getOrderCompletionRate());
                sheet.getRow(i + 7).getCell(5).setCellValue(businessData.getUnitPrice());
                sheet.getRow(i + 7).getCell(6).setCellValue(businessData.getNewUsers());

                currentDate = currentDate.minusDays(1);
            }

            // 3、将文件写入到浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            xssfWorkbook.write(outputStream);

            // 关闭资源
            inputStream.close();
            xssfWorkbook.close();
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
