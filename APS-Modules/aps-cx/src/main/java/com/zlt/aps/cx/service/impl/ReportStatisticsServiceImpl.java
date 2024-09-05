package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.IoUtils;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.cx.api.domain.dto.ReportStatisticsDto;
import com.zlt.aps.cx.mapper.ReportStatisticsMapper;
import com.zlt.aps.cx.service.ReportStatisticsService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author: Chen
 * @since: 2022/4/25 13:41
 */
@Service
public class ReportStatisticsServiceImpl implements ReportStatisticsService {

    @Value("${excelModelPath}")
    public String excelModelPath;
    @Autowired
    private ReportStatisticsMapper reportStatisticsMapper;

    /**
     * 根据条件查询报表统计列表
     */
    @Override
    public List<ReportStatisticsDto> selectReportStatisticsList(ReportStatisticsDto dto) {
        List<ReportStatisticsDto> list = null;
        if("1".equals(dto.getStatisticalMethod())) {
            //统计方式：每天
            list = reportStatisticsMapper.selectReportStatisticsList(dto);
        } else {
            //统计方式：汇总
            list = reportStatisticsMapper.selectReportSummaryList(dto);
        }
        list = addSummaryData(list);  //统计汇总记录
        return list;
    }

    /**
     * 计算每日汇总记录
     * @param list
     * @return
     */
    private List<ReportStatisticsDto> addSummaryData(List<ReportStatisticsDto> list) {
        if(list == null || list.isEmpty()) {
            return list;
        }
        List<ReportStatisticsDto> newList = new ArrayList<>();  //添加了“半部件汇总”的新集合
        LinkedHashMap<String, List<ReportStatisticsDto>> resultMap = new LinkedHashMap<>();  //根据排程日期对结果数据进行分组
        for(ReportStatisticsDto dto : list) {
           String key = DateUtil.formatDate(dto.getScheduleDate());
            List<ReportStatisticsDto> dayList = (resultMap.get(key) == null ? new ArrayList<ReportStatisticsDto>() : resultMap.get(key));
            dayList.add(dto);
            resultMap.put(key, dayList);
        }

        for(Map.Entry<String, List<ReportStatisticsDto>> entry : resultMap.entrySet()) {
            String scheduleDate = entry.getKey();
            List<ReportStatisticsDto> dayList = entry.getValue();

            ReportStatisticsDto summaryDto = new ReportStatisticsDto();  //半部件汇总的那条记录
            summaryDto.setScheduleDate(StringUtils.isBlank(scheduleDate) ? null : DateUtil.from(scheduleDate));
            summaryDto.setProcedureCode("-1");  //半部件汇总类型
            for(ReportStatisticsDto dto : dayList) {
                if("0".equals(dto.getProcedureCode()) || "1".equals(dto.getProcedureCode())) {
                    //过滤掉硫化、成型的数据，只汇总半部件的数据
                    continue;
                }
                summaryDto.setPlanProductionQty(BigDecimalUtil.add(summaryDto.getPlanProductionQty(), dto.getPlanProductionQty()));  //汇总计划生产量
                summaryDto.setActualProductionQty(BigDecimalUtil.add(summaryDto.getActualProductionQty(), dto.getActualProductionQty()));  //汇总实际生产量
                summaryDto.setActualProFinishRateLow(BigDecimalUtil.add(summaryDto.getActualProFinishRateLow(), dto.getActualProFinishRateLow()));  //汇总实 际生产规格完成率:X<90% 的规格数
                summaryDto.setActualProFinishRateMid(BigDecimalUtil.add(summaryDto.getActualProFinishRateMid(), dto.getActualProFinishRateMid()));  //汇总实 实际生产规格完成率:90%<X<110% 的规格数
                summaryDto.setActualProFinishRateHigh(BigDecimalUtil.add(summaryDto.getActualProFinishRateHigh(), dto.getActualProFinishRateHigh()));  //汇总实 实际生产规格完成率:X>110% 的规格数
                summaryDto.setTotalSpecifications(BigDecimalUtil.add(summaryDto.getTotalSpecifications(), dto.getTotalSpecifications()));  //汇总实 规格总量 的规格数
            }

            //计算半部件汇总记录的 生产完成率
            Double produceFinishRate = 0D;
            if(summaryDto.getPlanProductionQty() == 0) {
                summaryDto.setProduceFinishRate(100D);
            } else {
                produceFinishRate = BigDecimalUtil.div(summaryDto.getActualProductionQty(), summaryDto.getPlanProductionQty());
                produceFinishRate = BigDecimalUtil.mul(produceFinishRate, 100D);
                summaryDto.setProduceFinishRate(BigDecimalUtil.roundDown(produceFinishRate, 2));
            }
            dayList.add(summaryDto);
            newList.addAll(dayList);
        }
        return newList;
    }

    private Double getOrZero(Double value) {
        return value == null ? 0D : value;
    }

    /**
     * 导出报表统计数据
     */
    @Override
    public byte[] export(ReportStatisticsDto dto) {
        List<ReportStatisticsDto> list = this.selectReportStatisticsList(dto);
        //按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "reportStatistics.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "reportStatistics_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> procedureCodeMap = dto.getProcedureCodeMap();
            DecimalFormat df = new DecimalFormat("0.00%");
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            DataFormat format = webBook.createDataFormat();
            cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白
            Row row1 = sheet.getRow(0);
            for (int i = 0; i < list.size(); i++) {
                int n = 0;
                ReportStatisticsDto reportStatisticsDto = list.get(i);
                Row row = sheet.createRow(i + 2);
                row.createCell(n++).setCellValue(reportStatisticsDto.getScheduleDate() == null ? "" : DateUtils.parseDateToStr("yyyy-MM-dd", reportStatisticsDto.getScheduleDate()));
                row.createCell(n++).setCellValue(procedureCodeMap.getOrDefault(reportStatisticsDto.getProcedureCode(), I18nUtil.getMessage("ui.data.column.reportStatistics.halfParts")));
                row.createCell(n++).setCellValue(reportStatisticsDto.getPlanProductionQty() == null ? 0 : reportStatisticsDto.getPlanProductionQty());
                row.createCell(n++).setCellValue(reportStatisticsDto.getActualProductionQty() == null ? 0 : reportStatisticsDto.getActualProductionQty());
                row.createCell(n++).setCellValue(reportStatisticsDto.getProduceFinishRate() == null ? "" : reportStatisticsDto.getProduceFinishRate()+"%");
                row.createCell(n++).setCellValue(reportStatisticsDto.getActualProFinishRateLow() == null ? 0 : reportStatisticsDto.getActualProFinishRateLow());
                row.createCell(n++).setCellValue(reportStatisticsDto.getActualProFinishRateMid() == null ? 0 : reportStatisticsDto.getActualProFinishRateMid());
                row.createCell(n++).setCellValue(reportStatisticsDto.getActualProFinishRateHigh() == null ? 0 : reportStatisticsDto.getActualProFinishRateHigh());
                row.createCell(n++).setCellValue(reportStatisticsDto.getShiftPlanAccuracy() == null ? "" : df.format(reportStatisticsDto.getShiftPlanAccuracy()));
                row.createCell(n).setCellValue(reportStatisticsDto.getTotalSpecifications() == null ? 0 : reportStatisticsDto.getTotalSpecifications());

                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    if (j == 0) {
                        CellStyle cellStyle1 = webBook.createCellStyle();
                        cellStyle1.setAlignment(HorizontalAlignment.LEFT);
                        cellStyle1.setVerticalAlignment(VerticalAlignment.CENTER);
                        cellStyle1.setBorderBottom(BorderStyle.THIN);
                        cellStyle1.setBorderTop(BorderStyle.THIN);
                        cellStyle1.setBorderLeft(BorderStyle.THIN);
                        cellStyle1.setBorderRight(BorderStyle.THIN);
                        row.getCell(j).setCellStyle(cellStyle1);
                    } else {
                        row.getCell(j).setCellStyle(cellStyle);
                    }
                }
            }
        }
        //写出字节流
        ByteArrayOutputStream out = null;
        byte[] data = null;
        try {
            out = new ByteArrayOutputStream();
            webBook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IoUtils.closeQuietly(out);
        }
        return data;
    }
}
