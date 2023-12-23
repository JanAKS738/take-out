package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {

        Map map=new HashMap();
        map.put("begin",begin);
        map.put("end",end);

        //总订单数
        Integer totalOrderCount = orderMapper.countByMap(map);



        //营业额
        map.put("status", Orders.COMPLETED);
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countByMap(map);

        Double unitPrice=0.0;
        Double orderCompletionRate=0.0;
        if (totalOrderCount!=0&&validOrderCount!=0){
            //订单完成率
            orderCompletionRate=validOrderCount.doubleValue()/totalOrderCount;
            //平均客单价
            unitPrice=turnover/validOrderCount;
        }

        //新增用户数
        Integer newUsers = userMapper.countByMap(map);

        BusinessDataVO businessDataVO = BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();

        return businessDataVO;

    }

    /**
     * 查询订单管理数据
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {

        Map map=new HashMap();
        map.put("begin",LocalDateTime.now().with(LocalTime.MIN));
        //全部订单
        Integer allOrders = orderMapper.countByMap(map);
        //待接单
        map.put("status",Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.countByMap(map);
        //待派送
        map.put("status",Orders.CONFIRMED);
        Integer deliverOrders = orderMapper.countByMap(map);
        //已完成
        map.put("status",Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);
        //已取消
        map.put("status",Orders.CANCELLED);
        Integer canceledOrders = orderMapper.countByMap(map);
        //全部订单
//        map.put("status", null);
//        Integer allOrders = orderMapper.countByMap(map);

        OrderOverViewVO orderOverViewVO = OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliverOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(canceledOrders)
                .allOrders(allOrders)
                .build();
        return orderOverViewVO;
    }

    /**
     * 查询菜品总览
     * @return
     */
    @Override
    public DishOverViewVO getDishOverView() {
        Map map=new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sale = dishMapper.countByMap(map);
        map.put("status",StatusConstant.DISABLE);
        Integer nosale = dishMapper.countByMap(map);

        DishOverViewVO dishOverViewVO = DishOverViewVO.builder()
                .sold(sale)
                .discontinued(nosale)
                .build();


        return dishOverViewVO;
    }

    /**
     * 查询套餐总览
     * @return
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Map map=new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sale = setmealMapper.countByMap(map);
        map.put("status",StatusConstant.DISABLE);
        Integer nosale = setmealMapper.countByMap(map);

        SetmealOverViewVO setmealOverViewVO = SetmealOverViewVO.builder()
                .sold(sale)
                .discontinued(nosale)
                .build();


        return setmealOverViewVO;

    }
}
