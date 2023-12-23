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
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 统计指定时间内的营业额
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = creatDateList(begin, end);

        List<Double> turnoverList=new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover=orderMapper.sumByMap(map);
            turnover=turnover==null?0.0:turnover;
            turnoverList.add(turnover);
        }
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();

        return turnoverReportVO;
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = creatDateList(begin, end);
        //放每日新增用户数量
        List<Integer> newUserList=new ArrayList<>();
        //放每日总用户数量
        List<Integer> totalUserList=new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime=LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date,LocalTime.MAX);

            Map map=new HashMap();
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
        return userReportVO;


    }

    /**
     * 返回指定时间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        
        List<LocalDate> dateList = creatDateList(begin, end);
        //订单总数集合
        List<Integer> orderCountList=new ArrayList<>();
        //有效订单数集合
        List<Integer> validOrderCountList=new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //订单总数
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            //有效订单数
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //计算订单总数量
        Integer totalCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //订单完成率
        Double orderCompletionRate=0.0;
        if (totalCount!=0){orderCompletionRate=validCount.doubleValue()/totalCount;}

        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalCount)
                .validOrderCount(validCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
        return orderReportVO;
    }

    /**
     * 统计销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        SalesTop10ReportVO salesTop10ReportVO = SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();

        return salesTop10ReportVO;
    }

    /**
     * 导入数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        //查询所需数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);

        //通过POI加载器将数据写入模版中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //创建Excel文件
            XSSFWorkbook excel=new XSSFWorkbook(in);
            //开始填充数据
            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            sheet1.getRow(1).getCell(1).setCellValue("时间"+begin+"至"+end);
            sheet1.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet1.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet1.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
            sheet1.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            sheet1.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());
            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                sheet1.getRow(7+i).getCell(1).setCellValue(date.toString());
                sheet1.getRow(7+i).getCell(2).setCellValue(businessData.getTurnover());
                sheet1.getRow(7+i).getCell(3).setCellValue(businessData.getValidOrderCount());
                sheet1.getRow(7+i).getCell(4).setCellValue(businessData.getOrderCompletionRate());
                sheet1.getRow(7+i).getCell(5).setCellValue(businessData.getUnitPrice());
                sheet1.getRow(7+i).getCell(6).setCellValue(businessData.getNewUsers());
            }

            //通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            //关闭资源
            outputStream.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    /**
     * 返回所需要的日期列表
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> creatDateList(LocalDate begin,LocalDate end){
        List<LocalDate> dateList=new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }


    private Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map=new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);
        return orderMapper.countByMap(map);
    }

}
