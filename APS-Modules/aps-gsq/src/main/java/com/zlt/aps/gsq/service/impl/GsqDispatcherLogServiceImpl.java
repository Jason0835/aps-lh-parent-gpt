package com.zlt.aps.gsq.service.impl;

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
import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.mapper.GsqDispatcherLogMapper;
import com.zlt.aps.gsq.service.GsqDispatcherLogService;
import com.zlt.aps.gsq.service.GsqMachineInfoService;

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
     * 6班次制：操作前/后各6个班次计划量，变更的单元格以珊瑚色高亮
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
        // 机台编码 → 机台名称 映射（6班次制使用 machineCode 作为关联键）
        Map<String, String> machineMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
            machineMap = machineInfoList.stream()
                    .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, GsqMachineInfo::getMachineName, (k1, k2) -> k1));
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
                row.createCell(cellNum++).setCellValue(log.getSteelRingCode() == null ? "" : log.getSteelRingCode());
                row.createCell(cellNum++).setCellValue(log.getCreateBy() == null ? "" : log.getCreateBy());
                row.createCell(cellNum++).setCellValue(log.getCreateTime() == null ? "" : DateFormatUtils.format(log.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
                // 操作前机台（按机台编码反显机台名称）
                String beforeMachine = log.getBeforeMachineCode() == null ? "" : log.getBeforeMachineCode();
                String beforeMachineName = machineMap.getOrDefault(beforeMachine, beforeMachine);
                row.createCell(cellNum++).setCellValue(beforeMachineName);
                // 操作前6班次计划量
                Integer[] beforePlans = {
                        log.getBeforeClass1Plan(), log.getBeforeClass2Plan(), log.getBeforeClass3Plan(),
                        log.getBeforeClass4Plan(), log.getBeforeClass5Plan(), log.getBeforeClass6Plan()
                };
                for (Integer plan : beforePlans) {
                    row.createCell(cellNum++).setCellValue(plan == null ? 0 : plan);
                }
                // 操作后机台
                Cell afterMachineCell = row.createCell(cellNum++);
                String afterMachine = log.getAfterMachineCode() == null ? "" : log.getAfterMachineCode();
                String afterMachineName = machineMap.getOrDefault(afterMachine, afterMachine);
                afterMachineCell.setCellValue(afterMachineName);
                // 操作后6班次计划量
                Integer[] afterPlans = {
                        log.getAfterClass1Plan(), log.getAfterClass2Plan(), log.getAfterClass3Plan(),
                        log.getAfterClass4Plan(), log.getAfterClass5Plan(), log.getAfterClass6Plan()
                };
                Cell[] afterPlanCells = new Cell[6];
                for (int j = 0; j < 6; j++) {
                    afterPlanCells[j] = row.createCell(cellNum++);
                    afterPlanCells[j].setCellValue(afterPlans[j] == null ? 0 : afterPlans[j]);
                }
                //设置单元格样式
                int cellCount = row.getPhysicalNumberOfCells();
                for (int j = 0; j < cellCount; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
                }
                //机台变更高亮
                if (!beforeMachine.equals(afterMachine)) {
                    afterMachineCell.setCellStyle(redCellStyle);
                }
                //6班次计划量变更高亮
                for (int j = 0; j < 6; j++) {
                    int beforeVal = beforePlans[j] == null ? 0 : beforePlans[j];
                    int afterVal = afterPlans[j] == null ? 0 : afterPlans[j];
                    if (beforeVal != afterVal) {
                        afterPlanCells[j].setCellStyle(redCellStyle);
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
            IOUtils.closeQuietly(out);
        }
        return data;
    }
}
