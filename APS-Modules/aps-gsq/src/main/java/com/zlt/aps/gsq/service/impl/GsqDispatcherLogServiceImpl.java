package com.zlt.aps.gsq.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.mapper.GsqDispatcherLogMapper;
import com.zlt.aps.gsq.service.GsqDispatcherLogService;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
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
 * 钢丝圈调度员排程操作日志Service业务层处理
 * 
 * @author Gim
 * @date 2022-02-25
 */
@Service
public class GsqDispatcherLogServiceImpl implements GsqDispatcherLogService
{
    @Autowired
    private GsqDispatcherLogMapper gsqDispatcherLogMapper;

    @Autowired
    private GsqMachineInfoService machineInfoService;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询钢丝圈调度员排程操作日志
     * 
     * @param id 钢丝圈调度员排程操作日志ID
     * @return 钢丝圈调度员排程操作日志
     */
    @Override
    public GsqDispatcherLog selectGsqDispatcherLogById(Long id)
    {
        return gsqDispatcherLogMapper.selectGsqDispatcherLogById(id);
    }

    /**
     * 查询钢丝圈调度员排程操作日志列表
     * 
     * @param gsqDispatcherLog 钢丝圈调度员排程操作日志
     * @return 钢丝圈调度员排程操作日志
     */
    @Override
    public List<GsqDispatcherLog> selectGsqDispatcherLogList(GsqDispatcherLog dispatcherLog)
    {
        if (StringUtils.isNotEmpty(dispatcherLog.getEndTime())) {
            dispatcherLog.setEndTime(dispatcherLog.getEndTime() + " 23:59:59");
        }
        return gsqDispatcherLogMapper.selectGsqDispatcherLogList(dispatcherLog);
    }

    /**
     * 新增钢丝圈调度员排程操作日志
     * 
     * @param gsqDispatcherLog 钢丝圈调度员排程操作日志
     * @return 结果
     */
    @Override
    public int insertGsqDispatcherLog(GsqDispatcherLog gsqDispatcherLog)
    {
        gsqDispatcherLog.setBaseVale(null);
        return gsqDispatcherLogMapper.insertGsqDispatcherLog(gsqDispatcherLog);
    }

    /**
     * 导出Excel
     *
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    @Override
    public byte[] export(GsqDispatcherLog dispatcherLog) {
        List<GsqDispatcherLog> list = this.selectGsqDispatcherLogList(dispatcherLog);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath +
                I18nUtil.getMessage("ui.data.column.gsq.dispatcherlog.modelName") + ".xlsx");
        List<GsqMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(new GsqMachineInfo());
        Map<String, String> machineMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
            machineMap = machineInfoList.stream().collect(Collectors.toMap(item -> item.getId().toString(), GsqMachineInfo::getMachineName));
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
                GsqDispatcherLog log = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                String operType = log.getOperType();
                row.createCell(cellNum++).setCellValue(operType == null ? "" : operationTypeDictMap.getOrDefault(operType, ""));
                row.createCell(cellNum++).setCellValue(log.getScheduleDate() == null ? "" : DateFormatUtils.format(log.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(log.getMaterialCode() == null ? "" : log.getMaterialCode());
                row.createCell(cellNum++).setCellValue(log.getCreateBy() == null ? "" : log.getCreateBy());
                row.createCell(cellNum++).setCellValue(log.getCreateTime() == null ? "" : DateFormatUtils.format(log.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
                String beforeMachine = log.getBeforeMachineId() == null ? "" : log.getBeforeMachineId();
                String[] beforeMachineArr = beforeMachine.split(",");
                StringBuilder beforeMachineStr = new StringBuilder();
                for (String machineId : beforeMachineArr) {
                    beforeMachineStr.append(machineMap.getOrDefault(machineId, "")).append(",");
                }
                row.createCell(cellNum++).setCellValue(beforeMachineStr.substring(0, beforeMachineStr.length() - 1));
                double beforeMidPlan = log.getBeforeMidPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeMidPlan();
                row.createCell(cellNum++).setCellValue(beforeMidPlan);
                double beforeNightPlan = log.getBeforeNightPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeNightPlan();
                row.createCell(cellNum++).setCellValue(beforeNightPlan);
                double beforeDayPlan = log.getBeforeDayPlan() == null ? BigDecimal.ZERO.doubleValue() : log.getBeforeDayPlan();
                row.createCell(cellNum++).setCellValue(beforeDayPlan);
                Cell afterMachineCell = row.createCell(cellNum++);
                String afterMachine = log.getAfterMachineId() == null ? "" : log.getAfterMachineId();
                String[] afterMachineArr = afterMachine.split(",");
                StringBuilder afterMachineStr = new StringBuilder();
                for (String machineId : afterMachineArr) {
                    afterMachineStr.append(machineMap.getOrDefault(machineId, "")).append(",");
                }
                afterMachineCell.setCellValue(afterMachineStr.substring(0, afterMachineStr.length() - 1));
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
                if (!beforeMachine.equals(afterMachine)) {
                    afterMachineCell.setCellStyle(redCellStyle);
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
