package com.zlt.aps.tq.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.tq.api.domain.entity.TqDispatcherLog;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.mapper.TqDispatcherLogMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.TqDispatcherLogService;

/**
 * 胎圈调度员排程操作日志Service业务层处理
 *
 * <p>6班次制（v5）：1班=D日中班、2班=D+1日夜班、3班=D+1日早班、4班=D+1日中班、5班=D+2日夜班、6班=D+2日早班</p>
 *
 * @author Gim
 * @date 2022-02-25
 */
@Service
public class TqDispatcherLogServiceImpl implements TqDispatcherLogService
{
    @Autowired
    private TqDispatcherLogMapper tqDispatcherLogMapper;

    @Autowired
    private ITqMachineInfoService machineInfoService;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询胎圈调度员排程操作日志
     *
     * @param id 胎圈调度员排程操作日志ID
     * @return 胎圈调度员排程操作日志
     */
    @Override
    public TqDispatcherLog selectTqDispatcherLogById(Long id)
    {
        return tqDispatcherLogMapper.selectTqDispatcherLogById(id);
    }

    /**
     * 查询胎圈调度员排程操作日志列表
     *
     * @param tqDispatcherLog 胎圈调度员排程操作日志
     * @return 胎圈调度员排程操作日志
     */
    @Override
    public List<TqDispatcherLog> selectTqDispatcherLogList(TqDispatcherLog dispatcherLog)
    {
        if (StringUtils.isNotEmpty(dispatcherLog.getEndTime())) {
            dispatcherLog.setEndTime(dispatcherLog.getEndTime() + " 23:59:59");
        }
        return tqDispatcherLogMapper.selectTqDispatcherLogList(dispatcherLog);
    }

    /**
     * 新增胎圈调度员排程操作日志
     *
     * @param tqDispatcherLog 胎圈调度员排程操作日志
     * @return 结果
     */
    @Override
    public int insertTqDispatcherLog(TqDispatcherLog tqDispatcherLog)
    {
        tqDispatcherLog.setBaseVale(null);
        return tqDispatcherLogMapper.insertTqDispatcherLog(tqDispatcherLog);
    }

    /**
     * 导出Excel（6班次制）
     *
     * <p>导出列顺序：操作类型、排程日期、胎圈代码、操作人、操作时间、
     * 操作前机台编码、操作前1~6班计划量、操作后机台编码、操作后1~6班计划量。
     * 操作前后值不一致的单元格高亮显示（CORAL色）。</p>
     *
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    @Override
    public byte[] export(TqDispatcherLog dispatcherLog) {
        List<TqDispatcherLog> list = this.selectTqDispatcherLogList(dispatcherLog);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath +
                I18nUtil.getMessage("ui.data.column.tq.dispatcherlog.modelName") + ".xlsx");
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
                TqDispatcherLog log = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                // 基础信息列
                String operType = log.getOperType();
                row.createCell(cellNum++).setCellValue(operType == null ? "" : operationTypeDictMap.getOrDefault(operType, ""));
                row.createCell(cellNum++).setCellValue(log.getScheduleDate() == null ? "" : DateFormatUtils.format(log.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(log.getBeadCode() == null ? "" : log.getBeadCode());
                row.createCell(cellNum++).setCellValue(log.getCreateBy() == null ? "" : log.getCreateBy());
                row.createCell(cellNum++).setCellValue(log.getCreateTime() == null ? "" : DateFormatUtils.format(log.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
                // 操作前机台编码（v5直接使用机台编码，不再做ID→名称转换）
                String beforeMachineCode = log.getBeforeMachineCode() == null ? "" : log.getBeforeMachineCode();
                row.createCell(cellNum++).setCellValue(beforeMachineCode);
                // 操作前6班次计划量
                int beforeClass1 = log.getBeforeClass1Plan() == null ? 0 : log.getBeforeClass1Plan();
                int beforeClass2 = log.getBeforeClass2Plan() == null ? 0 : log.getBeforeClass2Plan();
                int beforeClass3 = log.getBeforeClass3Plan() == null ? 0 : log.getBeforeClass3Plan();
                int beforeClass4 = log.getBeforeClass4Plan() == null ? 0 : log.getBeforeClass4Plan();
                int beforeClass5 = log.getBeforeClass5Plan() == null ? 0 : log.getBeforeClass5Plan();
                int beforeClass6 = log.getBeforeClass6Plan() == null ? 0 : log.getBeforeClass6Plan();
                row.createCell(cellNum++).setCellValue(beforeClass1);
                row.createCell(cellNum++).setCellValue(beforeClass2);
                row.createCell(cellNum++).setCellValue(beforeClass3);
                row.createCell(cellNum++).setCellValue(beforeClass4);
                row.createCell(cellNum++).setCellValue(beforeClass5);
                row.createCell(cellNum++).setCellValue(beforeClass6);
                // 操作后机台编码
                Cell afterMachineCell = row.createCell(cellNum++);
                String afterMachineCode = log.getAfterMachineCode() == null ? "" : log.getAfterMachineCode();
                afterMachineCell.setCellValue(afterMachineCode);
                // 操作后6班次计划量
                Cell afterClass1Cell = row.createCell(cellNum++);
                int afterClass1 = log.getAfterClass1Plan() == null ? 0 : log.getAfterClass1Plan();
                afterClass1Cell.setCellValue(afterClass1);
                Cell afterClass2Cell = row.createCell(cellNum++);
                int afterClass2 = log.getAfterClass2Plan() == null ? 0 : log.getAfterClass2Plan();
                afterClass2Cell.setCellValue(afterClass2);
                Cell afterClass3Cell = row.createCell(cellNum++);
                int afterClass3 = log.getAfterClass3Plan() == null ? 0 : log.getAfterClass3Plan();
                afterClass3Cell.setCellValue(afterClass3);
                Cell afterClass4Cell = row.createCell(cellNum++);
                int afterClass4 = log.getAfterClass4Plan() == null ? 0 : log.getAfterClass4Plan();
                afterClass4Cell.setCellValue(afterClass4);
                Cell afterClass5Cell = row.createCell(cellNum++);
                int afterClass5 = log.getAfterClass5Plan() == null ? 0 : log.getAfterClass5Plan();
                afterClass5Cell.setCellValue(afterClass5);
                Cell afterClass6Cell = row.createCell(cellNum);
                int afterClass6 = log.getAfterClass6Plan() == null ? 0 : log.getAfterClass6Plan();
                afterClass6Cell.setCellValue(afterClass6);
                //设置单元格样式
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
                }
                //操作前后值不一致的单元格高亮显示
                if (!beforeMachineCode.equals(afterMachineCode)) {
                    afterMachineCell.setCellStyle(redCellStyle);
                }
                if (beforeClass1 != afterClass1) {
                    afterClass1Cell.setCellStyle(redCellStyle);
                }
                if (beforeClass2 != afterClass2) {
                    afterClass2Cell.setCellStyle(redCellStyle);
                }
                if (beforeClass3 != afterClass3) {
                    afterClass3Cell.setCellStyle(redCellStyle);
                }
                if (beforeClass4 != afterClass4) {
                    afterClass4Cell.setCellStyle(redCellStyle);
                }
                if (beforeClass5 != afterClass5) {
                    afterClass5Cell.setCellStyle(redCellStyle);
                }
                if (beforeClass6 != afterClass6) {
                    afterClass6Cell.setCellStyle(redCellStyle);
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
