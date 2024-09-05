package com.zlt.aps.lh.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.mapper.LhDispatcherLogMapper;
import com.zlt.aps.lh.service.LhDispatcherLogService;
import com.zlt.aps.lh.service.LhMachineInfoService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 硫化调度员排程操作日志Service业务层处理
 * 
 * @author Gim
 * @date 2022-02-25
 */
@Service
public class LhDispatcherLogServiceImpl implements LhDispatcherLogService
{
    @Autowired
    private LhDispatcherLogMapper lhDispatcherLogMapper;

    @Autowired
    private LhMachineInfoService machineInfoService;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询硫化调度员排程操作日志
     * 
     * @param id 硫化调度员排程操作日志ID
     * @return 硫化调度员排程操作日志
     */
    @Override
    public LhDispatcherLog selectLhDispatcherLogById(Long id)
    {
        return lhDispatcherLogMapper.selectLhDispatcherLogById(id);
    }

    /**
     * 查询硫化调度员排程操作日志列表
     * 
     * @param lhDispatcherLog 硫化调度员排程操作日志
     * @return 硫化调度员排程操作日志
     */
    @Override
    public List<LhDispatcherLog> selectLhDispatcherLogList(LhDispatcherLog dispatcherLog)
    {
        if (StringUtils.isNotEmpty(dispatcherLog.getEndTime())) {
            dispatcherLog.setEndTime(dispatcherLog.getEndTime() + " 23:59:59");
        }
        return lhDispatcherLogMapper.selectLhDispatcherLogList(dispatcherLog);
    }

    /**
     * 新增硫化调度员排程操作日志
     * 
     * @param lhDispatcherLog 硫化调度员排程操作日志
     * @return 结果
     */
    @Override
    public int insertLhDispatcherLog(LhDispatcherLog lhDispatcherLog)
    {
        lhDispatcherLog.setBaseVale(null);
        return lhDispatcherLogMapper.insertLhDispatcherLog(lhDispatcherLog);
    }

    /**
     * 导出Excel
     *
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    @Override
    public byte[] export(LhDispatcherLog dispatcherLog) {
        List<LhDispatcherLog> list = this.selectLhDispatcherLogList(dispatcherLog);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath +
                I18nUtil.getMessage("ui.data.column.lh.dispatcherlog.modelName") + ".xlsx");
        // 查询硫化机信息
        List<LhMachineInfo> lhMachineInfoList = machineInfoService.selectMachineInfoList(new LhMachineInfo());
        Map<String, String> lhMachineMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(lhMachineInfoList)) {
            lhMachineMap = lhMachineInfoList.stream().collect(Collectors.toMap(LhMachineInfo::getMachineCode, LhMachineInfo::getMachineName));
        }
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
                LhDispatcherLog log = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                String operType = log.getOperType();
                row.createCell(cellNum++).setCellValue(operType == null ? "" : operationTypeDictMap.getOrDefault(operType, ""));
                row.createCell(cellNum++).setCellValue(log.getScheduleDate() == null ? "" : DateFormatUtils.format(log.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(log.getSapCode() == null ? "" : log.getSapCode());
                row.createCell(cellNum++).setCellValue(log.getCreateBy() == null ? "" : log.getCreateBy());
                row.createCell(cellNum++).setCellValue(log.getCreateTime() == null ? "" : DateFormatUtils.format(log.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
                // 操作前硫化机台名称
                String beforeLhMachine = log.getBeforeMachineCode() == null ? "" : log.getBeforeMachineCode();
                String[] beforeLhMachineArr = beforeLhMachine.split(",");
                StringBuilder beforeLhMachineStr = new StringBuilder();
                for (String machineId : beforeLhMachineArr) {
                    beforeLhMachineStr.append(lhMachineMap.getOrDefault(machineId, "")).append(",");
                }
                row.createCell(cellNum++).setCellValue(beforeLhMachineStr.substring(0, beforeLhMachineStr.length() - 1));
                // 操作前计划量
                double beforeClass1Plan = log.getBeforeClass1Plan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeClass1Plan();
                row.createCell(cellNum++).setCellValue(beforeClass1Plan);
                double beforeClass2Plan = log.getBeforeClass2Plan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeClass2Plan();
                row.createCell(cellNum++).setCellValue(beforeClass2Plan);
                double beforeClass3Plan = log.getBeforeClass3Plan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeClass3Plan();
                row.createCell(cellNum++).setCellValue(beforeClass3Plan);
                // 操作后硫化机台名称
                Cell afterLhMachineCell = row.createCell(cellNum++);
                String afterLhMachine = log.getAfterMachineCode() == null ? "" : log.getAfterMachineCode();
                String[] afterLhMachineArr = afterLhMachine.split(",");
                StringBuilder afterLhMachineStr = new StringBuilder();
                for (String machineId : afterLhMachineArr) {
                    afterLhMachineStr.append(lhMachineMap.getOrDefault(machineId, "")).append(",");
                }
                afterLhMachineCell.setCellValue(afterLhMachineStr.substring(0, afterLhMachineStr.length() - 1));
                // 操作后计划量
                Cell afterClass1PlanCell = row.createCell(cellNum++);
                double afterClass1Plan = log.getAfterClass1Plan() == null ? BigDecimal.ZERO.doubleValue() : log.getAfterClass1Plan();
                afterClass1PlanCell.setCellValue(afterClass1Plan);
                Cell afterClass2PlanCell = row.createCell(cellNum++);
                double afterClass2Plan = log.getAfterClass2Plan() == null ? BigDecimal.ZERO.doubleValue() : log.getAfterClass2Plan();
                afterClass2PlanCell.setCellValue(afterClass2Plan);
                Cell afterClass3PlanCell = row.createCell(cellNum);
                double afterClass3Plan = log.getAfterClass3Plan() == null ? BigDecimal.ZERO.doubleValue() : log.getAfterClass3Plan();
                afterClass3PlanCell.setCellValue(afterClass3Plan);
                //设置单元格样式
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
                }
                if (!beforeLhMachine.equals(afterLhMachine)) {
                    afterLhMachineCell.setCellStyle(redCellStyle);
                }
                if (!(beforeClass1Plan == afterClass1Plan)) {
                    afterClass1PlanCell.setCellStyle(redCellStyle);
                }
                if (!(beforeClass2Plan == afterClass2Plan)) {
                    afterClass2PlanCell.setCellStyle(redCellStyle);
                }
                if (!(beforeClass3Plan == afterClass3Plan)) {
                    afterClass3PlanCell.setCellStyle(redCellStyle);
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
