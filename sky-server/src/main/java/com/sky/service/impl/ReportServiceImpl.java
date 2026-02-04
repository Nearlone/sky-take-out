package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
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
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 根据时间区间统计营业额
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);//日期计算，获得指定日期后1天的日期
            dateList.add(begin);
        }
        
       List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
        	map.put("status", Orders.COMPLETED);
        	map.put("begin",beginTime);
        	map.put("end", endTime);
            Double turnover = orderMapper.sumByMap(map); 
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        //数据封装
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> newUserList = new ArrayList<>(); //新增用户数
        List<Integer> totalUserList = new ArrayList<>(); //总用户数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //新增用户数量 select count(id) from user where create_time > ? and create_time < ?
            Integer newUser = getUserCount(beginTime, endTime);
            //总用户数量 select count(id) from user where  create_time < ?
            Integer totalUser = getUserCount(null, endTime);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    /**
     * 根据时间区间统计用户数量
     * @param beginTime
     * @param endTime
     * @return
     */
    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Map map = new HashMap();
        map.put("begin",beginTime);
        map.put("end", endTime);
        return userMapper.countByMap(map);
    }

    /**
     * 根据时间区间统计订单数量
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //每天订单总数集合
        List<Integer> orderCountList = new ArrayList<>();
        //每天有效订单数集合
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天的总订单数 select count(id) from orders where order_time > ? and order_time < ?
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = ?
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //时间区间内的总订单数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //时间区间内的总有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }

    /**
     * 根据时间区间统计指定状态的订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("status", status);
        map.put("begin",beginTime);
        map.put("end", endTime);
        return orderMapper.countByMap(map);
    }

    /**
     * 查询指定时间区间内的销量排名top10
     * @param begin
     * @param end
     * @return
     * */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end){
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()),",");
        String numberList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()),",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**导出近30天的运营数据报表
     * @param response
     **/
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //查询概览运营数据，提供给Excel模板文件
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        
        // 检查模板文件是否存在
        if (inputStream == null) {
            log.error("Excel模板文件未找到: template/运营数据报表模板.xlsx");
            // 创建简单的Excel文件作为替代方案
            createSimpleExcel(response, begin, end, businessData);
            return;
        }
        
        try {
            //基于提供好的模板文件创建一个新的Excel表格对象
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //获得Excel文件中的一个Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            //获得第4行
            XSSFRow row = sheet.getRow(3);
            //获取单元格
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //准备明细数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            //通过输出流将文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.flush();
            out.close();
            excel.close();
            inputStream.close();

        }catch (IOException e){
            log.error("导出Excel文件时发生错误", e);
            e.printStackTrace();
        }
    }

    /**
     * 创建简单的Excel文件作为模板缺失时的替代方案
     * @param response
     * @param begin
     * @param end
     * @param businessData
     */
    private void createSimpleExcel(HttpServletResponse response, LocalDate begin, LocalDate end, BusinessDataVO businessData) {
        try {
            // 创建新的工作簿
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("运营数据报表");
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=运营数据报表.xlsx");
            
            // 创建标题行
            XSSFRow titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("运营数据报表");
            
            // 时间范围行
            XSSFRow dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("统计时间范围：");
            dateRow.createCell(1).setCellValue(begin + " 至 " + end);
            
            // 概览数据标题行
            XSSFRow overviewTitleRow = sheet.createRow(3);
            overviewTitleRow.createCell(0).setCellValue("数据概览");
            
            // 概览数据行
            XSSFRow overviewRow = sheet.createRow(4);
            overviewRow.createCell(0).setCellValue("营业额：");
            overviewRow.createCell(1).setCellValue(businessData.getTurnover() != null ? businessData.getTurnover() : 0.0);
            
            overviewRow = sheet.createRow(5);
            overviewRow.createCell(0).setCellValue("有效订单数：");
            overviewRow.createCell(1).setCellValue(businessData.getValidOrderCount() != null ? businessData.getValidOrderCount() : 0);
            
            overviewRow = sheet.createRow(6);
            overviewRow.createCell(0).setCellValue("订单完成率：");
            overviewRow.createCell(1).setCellValue(businessData.getOrderCompletionRate() != null ? businessData.getOrderCompletionRate() : 0.0);
            
            overviewRow = sheet.createRow(7);
            overviewRow.createCell(0).setCellValue("平均客单价：");
            overviewRow.createCell(1).setCellValue(businessData.getUnitPrice() != null ? businessData.getUnitPrice() : 0.0);
            
            overviewRow = sheet.createRow(8);
            overviewRow.createCell(0).setCellValue("新增用户数：");
            overviewRow.createCell(1).setCellValue(businessData.getNewUsers() != null ? businessData.getNewUsers() : 0);
            
            // 明细数据标题行
            XSSFRow detailTitleRow = sheet.createRow(10);
            detailTitleRow.createCell(0).setCellValue("日期");
            detailTitleRow.createCell(1).setCellValue("营业额");
            detailTitleRow.createCell(2).setCellValue("有效订单数");
            detailTitleRow.createCell(3).setCellValue("订单完成率");
            detailTitleRow.createCell(4).setCellValue("平均客单价");
            detailTitleRow.createCell(5).setCellValue("新增用户数");
            
            // 填充30天的明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO dailyData = workspaceService.getBusinessData(
                    LocalDateTime.of(date, LocalTime.MIN), 
                    LocalDateTime.of(date, LocalTime.MAX)
                );
                
                XSSFRow detailRow = sheet.createRow(11 + i);
                detailRow.createCell(0).setCellValue(date.toString());
                detailRow.createCell(1).setCellValue(dailyData.getTurnover() != null ? dailyData.getTurnover() : 0.0);
                detailRow.createCell(2).setCellValue(dailyData.getValidOrderCount() != null ? dailyData.getValidOrderCount() : 0);
                detailRow.createCell(3).setCellValue(dailyData.getOrderCompletionRate() != null ? dailyData.getOrderCompletionRate() : 0.0);
                detailRow.createCell(4).setCellValue(dailyData.getUnitPrice() != null ? dailyData.getUnitPrice() : 0.0);
                detailRow.createCell(5).setCellValue(dailyData.getNewUsers() != null ? dailyData.getNewUsers() : 0);
            }
            
            // 写入响应
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            out.close();
            workbook.close();
            
        } catch (IOException e) {
            log.error("创建简单Excel文件时发生错误", e);
            e.printStackTrace();
        }
    }

}