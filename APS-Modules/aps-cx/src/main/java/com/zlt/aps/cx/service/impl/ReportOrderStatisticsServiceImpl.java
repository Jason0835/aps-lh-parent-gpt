package com.zlt.aps.cx.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.api.domain.dto.ReportOrderStatisticsDto;
import com.zlt.aps.cx.mapper.ReportOrderStatisticsMapper;
import com.zlt.aps.cx.service.ReportOrderStatisticsService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工单完成统计报表Service
 * @author: Chen
 * @since: 2022/8/10 10:29
 */
@Service
public class ReportOrderStatisticsServiceImpl implements ReportOrderStatisticsService {

    @Resource
    private ReportOrderStatisticsMapper reportOrderStatisticsMapper;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 根据条件查询报表统计列表（统计方式：每天）
     *
     * @param dto 参数
     */
    @Override
    public List<ReportOrderStatisticsDto> selectReportStatisticsList(ReportOrderStatisticsDto dto) {
        return reportOrderStatisticsMapper.selectReportStatisticsList(dto);
    }

    /**
     * 根据条件查询报表统计列表（统计方式：汇总）
     *
     * @param dto 参数
     */
    @Override
    public List<ReportOrderStatisticsDto> selectReportSummaryList(ReportOrderStatisticsDto dto) {
        return reportOrderStatisticsMapper.selectReportSummaryList(dto);
    }

    /**
     * 导出报表统计列表
     *
     * @param statisticsDto 参数
     */
    @Override
    public byte[] export(ReportOrderStatisticsDto statisticsDto) {
        //查询数据
        List<ReportOrderStatisticsDto> list;
        // 根据汇总方式查询数据
        if ("1".equals(statisticsDto.getStatisticalMethod())) {
            list = this.selectReportStatisticsList(statisticsDto);
        }else {
            list = this.selectReportSummaryList(statisticsDto);
        }
        String tempName = I18nUtil.getMessage("ui.data.column.reportOrderStatistics.modelName");
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + tempName + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);

        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            CellStyle yellowCellStyle = ExcelUtils.createCellStyle(webBook);
            yellowCellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            yellowCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Map<String, String> procedureCodeMap = statisticsDto.getProcedureCodeMap();
            String summary = I18nUtil.getMessage("ui.data.column.reportOrderStatistics.summary");
            for (int i = 0; i < list.size(); i++) {
                ReportOrderStatisticsDto dto = list.get(i);
                Row row = sheet.createRow(i + 2);
                int rowNum = 0;
                boolean isSummary = (dto.getDataType() != null && "2".equals(dto.getDataType()));
                String procedure = procedureCodeMap.getOrDefault(dto.getProcedureCode(), "");

                row.createCell(rowNum++).setCellValue(dto.getScheduleDate() == null ? "" : dto.getScheduleDate());
                row.createCell(rowNum++).setCellValue(isSummary ? procedure + summary : procedure);
                row.createCell(rowNum++).setCellValue(dto.getOrderNo() == null ? "" : dto.getOrderNo());
                row.createCell(rowNum++).setCellValue(dto.getSpecCode() == null ? "" : dto.getSpecCode());
                row.createCell(rowNum++).setCellValue(dto.getPlanProductionQty() == null ? BigDecimal.ZERO.doubleValue() : dto.getPlanProductionQty());
                row.createCell(rowNum++).setCellValue(dto.getActualFinishQty() == null ? BigDecimal.ZERO.doubleValue() : dto.getActualFinishQty());
                row.createCell(rowNum).setCellValue(dto.getFinishRate() == null ? "" : dto.getFinishRate());

                //设置单元格样式
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    row.getCell(j).setCellStyle(isSummary ? yellowCellStyle : cellStyle);
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
            IOUtils.closeQuietly(out);
        }
        return data;
    }
}
