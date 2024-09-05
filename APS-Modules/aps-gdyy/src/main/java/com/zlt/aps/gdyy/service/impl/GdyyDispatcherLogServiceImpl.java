package com.zlt.aps.gdyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.gdyy.api.domain.entity.GdyyDispatcherLog;
import com.zlt.aps.gdyy.mapper.GdyyDispatcherLogMapper;
import com.zlt.aps.gdyy.service.GdyyDispatcherLogService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 钢带压延调度员排程操作日志Service业务层处理
 * 
 * @author Gim
 * @date 2022-02-25
 */
@Service
public class GdyyDispatcherLogServiceImpl implements GdyyDispatcherLogService
{
    @Autowired
    private GdyyDispatcherLogMapper gdyyDispatcherLogMapper;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询钢带压延调度员排程操作日志
     * 
     * @param id 钢带压延调度员排程操作日志ID
     * @return 钢带压延调度员排程操作日志
     */
    @Override
    public GdyyDispatcherLog selectGdyyDispatcherLogById(Long id)
    {
        return gdyyDispatcherLogMapper.selectGdyyDispatcherLogById(id);
    }

    /**
     * 查询钢带压延调度员排程操作日志列表
     * 
     * @param gdyyDispatcherLog 钢带压延调度员排程操作日志
     * @return 钢带压延调度员排程操作日志
     */
    @Override
    public List<GdyyDispatcherLog> selectGdyyDispatcherLogList(GdyyDispatcherLog dispatcherLog)
    {
        if (StringUtils.isNotEmpty(dispatcherLog.getEndTime())) {
            dispatcherLog.setEndTime(dispatcherLog.getEndTime() + " 23:59:59");
        }
        return gdyyDispatcherLogMapper.selectGdyyDispatcherLogList(dispatcherLog);
    }

    /**
     * 新增钢带压延调度员排程操作日志
     * 
     * @param gdyyDispatcherLog 钢带压延调度员排程操作日志
     * @return 结果
     */
    @Override
    public int insertGdyyDispatcherLog(GdyyDispatcherLog gdyyDispatcherLog)
    {
        gdyyDispatcherLog.setBaseVale(null);
        return gdyyDispatcherLogMapper.insertGdyyDispatcherLog(gdyyDispatcherLog);
    }

    /**
     * 导出Excel
     *
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    @Override
    public byte[] export(GdyyDispatcherLog dispatcherLog) {
        List<GdyyDispatcherLog> list = this.selectGdyyDispatcherLogList(dispatcherLog);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath +
                I18nUtil.getMessage("ui.data.column.gdyy.dispatcherlog.modelName") + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> operationTypeDictMap = dispatcherLog.getOperationTypeDictMap();
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            CellStyle redCellStyle = ExcelUtils.createCellStyle(webBook);
            redCellStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            redCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < list.size(); i++) {
                GdyyDispatcherLog log = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                String operType = log.getOperType();
                row.createCell(cellNum++).setCellValue(operType == null ? "" : operationTypeDictMap.getOrDefault(operType, ""));
                row.createCell(cellNum++).setCellValue(log.getScheduleDate() == null ? "" : DateFormatUtils.format(log.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(log.getMaterialCode() == null ? "" : log.getMaterialCode());
                row.createCell(cellNum++).setCellValue(log.getCreateBy() == null ? "" : log.getCreateBy());
                row.createCell(cellNum++).setCellValue(log.getCreateTime() == null ? "" : DateFormatUtils.format(log.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
                double beforeMidPlan = log.getBeforeMidPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeMidPlan();
                row.createCell(cellNum++).setCellValue(beforeMidPlan);
                double beforeNightPlan = log.getBeforeNightPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeNightPlan();
                row.createCell(cellNum++).setCellValue(beforeNightPlan);
                double beforeDayPlan = log.getBeforeDayPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeDayPlan();
                row.createCell(cellNum++).setCellValue(beforeDayPlan);
                Cell afterMidPlanCell = row.createCell(cellNum++);
                double afterMidPlan = log.getAfterMidPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getAfterMidPlan();
                afterMidPlanCell.setCellValue(afterMidPlan);
                Cell afterNightPlanCell = row.createCell(cellNum++);
                double afterNightPlan = log.getAfterNightPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getAfterNightPlan();
                afterNightPlanCell.setCellValue(afterNightPlan);
                Cell afterDayPlanCell = row.createCell(cellNum);
                double afterDayPlan = log.getAfterDayPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getAfterDayPlan();
                afterDayPlanCell.setCellValue(afterDayPlan);
                //设置单元格样式
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
                }
                if (!(beforeMidPlan == afterMidPlan)) {
                    afterMidPlanCell.setCellStyle(redCellStyle);
                }
                if (!(beforeNightPlan == afterNightPlan)) {
                    afterNightPlanCell.setCellStyle(redCellStyle);
                }
                if (!(beforeDayPlan == afterDayPlan)) {
                    afterDayPlanCell.setCellStyle(redCellStyle);
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
