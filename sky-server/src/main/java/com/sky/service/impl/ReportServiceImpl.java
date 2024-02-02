package com.sky.service.impl;


import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;
    /**
     * 统计指定时间区间的营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
         while(!begin.equals(end)){
             begin = begin.plusDays(1);
             dateList.add(begin);
         }

         //存放每天的营业额
        List<Double> turnoveList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询状态为已完成的订单
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoveList.add(turnover);

        }

         return TurnoverReportVO
                 .builder()
                 .dateList(StringUtils.join(dateList, ","))
                 .turnoverList(StringUtils.join(turnoveList,","))
                 .build();

    }

    /**
     * 统计指定时间区间的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("end", endTime);

            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);

            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        //有效订单数和订单总数
        for(LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);

        }

        //订单总数量 有效订单数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据条件统计订单数量
     * @param begin
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap<>();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

    /**
     * 销量排名前10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop(beginTime,endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names,",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers,",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

}
