package com.zlt.mix.schedule.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import javax.annotation.Resource;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtils;
import com.zlt.mix.schedule.api.domain.dto.MaterialScheduleResultExportDictDto;
import com.zlt.mix.schedule.api.domain.dto.ScheduleOperLogDto;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.service.GlueScheduleResultService;
import com.zlt.mix.schedule.service.MaterialScheduleResultService;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.zlt.mix.schedule.mapper.ScheduleOperLogMapper;
import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;
import com.zlt.mix.schedule.service.ScheduleOperLogService;

/**
 * 排程操作日志Service业务层处理
 *
 * @author chen
 * @date 2022-07-13
 */
@Service
public class ScheduleOperLogServiceImpl extends ServiceImpl<ScheduleOperLogMapper, ScheduleOperLog> implements ScheduleOperLogService {
    @Resource
    private ScheduleOperLogMapper scheduleOperLogMapper;
    @Resource
    private GlueScheduleResultService glueScheduleResultService;
    @Resource
    private MaterialScheduleResultService materialScheduleResultService;
    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询排程操作日志列表
     *
     * @param scheduleOperLog 排程操作日志
     * @return 排程操作日志
     */
    @Override
    public List<ScheduleOperLog> selectScheduleOperLogList(ScheduleOperLog scheduleOperLog) {
        List<ScheduleOperLog> list = scheduleOperLogMapper.selectScheduleOperLogList(scheduleOperLog);
        // 将配方类型匹配后并赋值给配方类型名称
        setRecipeTypeName(list);
        return list;
    }

    /**
     * 保存排程操作日志信息（id为空则新增，id不为空则修改）
     *
     * @param scheduleOperLog 要操作的记录
     */
    @Override
    public void saveScheduleOperLog(ScheduleOperLog scheduleOperLog) {
        scheduleOperLog.setBaseValue(scheduleOperLog.getId());
        this.saveOrUpdate(scheduleOperLog);
    }

    /**
     * 批量插入排程操作日志
     * @param list 要插入的记录
     */
    @Override
    public void batchInsertScheduleOperLogInfo(List<ScheduleOperLog> list) {
        scheduleOperLogMapper.batchInsertScheduleOperLogInfo(list);
    }

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    @Override
    public byte[] exportData(ScheduleOperLogDto dto) {
        List<ScheduleOperLog> list = this.selectScheduleOperLogList(dto);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + I18nUtil.getMessage("schedule.scheduleOperLog.modelName") + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> mixAreaDictMap = dto.getMixAreaDictMap();
            Map<String, String> operTypeDictMap = dto.getOperTypeDictMap();
            Map<String, String> recipeStageDictMap = dto.getRecipeStageDictMap();
            String scheduleType = dto.getScheduleType();
            Map<String, String> machineNameMap = new HashMap<>();
            if (ZltConstant.OPER_SCHEDULE_TYPE_GLUE.equals(scheduleType)) {
                List<MixMachine> mixMachines = glueScheduleResultService.getMachineInfo(new MixMachine());
                machineNameMap = mixMachines.stream().collect(Collectors.toMap(item -> item.getMixArea() + item.getMachineCode(), MixMachine::getMachineName));
            }else if (ZltConstant.OPER_SCHEDULE_TYPE_MATERIAL.equals(scheduleType)) {
                List<LhflMachine> mixMachines = materialScheduleResultService.getMachineInfo(new LhflMachine());
                machineNameMap = mixMachines.stream().collect(Collectors.toMap(item -> item.getMixArea() + item.getMachineCode(), LhflMachine::getMachineName));
            }
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            for (int i = 0; i < list.size(); i++) {
                ScheduleOperLog scheduleOperLog = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                String mixArea = scheduleOperLog.getMixArea();
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getOperType() == null ? "" : operTypeDictMap.getOrDefault(scheduleOperLog.getOperType(), ""));
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getScheduleDate() == null ? "" : DateFormatUtils.format(scheduleOperLog.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(mixArea == null ? "" : mixAreaDictMap.getOrDefault(mixArea, ""));
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getMaterialCode() == null ? "" : scheduleOperLog.getMaterialCode());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getCreateBy() == null ? "" : scheduleOperLog.getCreateBy());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getCreateTime() == null ? "" : DateFormatUtils.format(scheduleOperLog.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));

                // 操作前机台code转换为机台名称导出
                String beforeMachineCode = scheduleOperLog.getBeforeMachineCode();
                StringBuilder beforeMachineName = new StringBuilder();
                if (StringUtil.isNotBlank(beforeMachineCode)) {
                    String[] beforeMachineCodeArr = beforeMachineCode.split(",");
                    for (String machineCode : beforeMachineCodeArr) {
                        beforeMachineName.append(machineNameMap.getOrDefault(mixArea + machineCode, ""))
                                .append(",");
                    }
                }
                row.createCell(cellNum++).setCellValue(beforeMachineCode == null ? "" : beforeMachineName.substring(0, beforeMachineName.length() - 1));
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeRecipeTypeName() == null ? "" : scheduleOperLog.getBeforeRecipeTypeName());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeRecipeVersionId() == null ? "" : scheduleOperLog.getBeforeRecipeVersionId());
                String beforeRecipeStage = scheduleOperLog.getBeforeRecipeStage();
                row.createCell(cellNum++).setCellValue(beforeRecipeStage == null ? "" : recipeStageDictMap.getOrDefault(beforeRecipeStage, ""));
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeMidPlan() == null ? BigDecimal.ZERO.doubleValue() : scheduleOperLog.getBeforeMidPlan());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeMidOrder() == null ? BigDecimal.ZERO.intValue() : scheduleOperLog.getBeforeMidOrder());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeNightPlan() == null ? BigDecimal.ZERO.doubleValue() : scheduleOperLog.getBeforeNightPlan());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeNightOrder() == null ? BigDecimal.ZERO.intValue() : scheduleOperLog.getBeforeNightOrder());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeDayPlan() == null ? BigDecimal.ZERO.doubleValue() : scheduleOperLog.getBeforeDayPlan());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getBeforeDayOrder() == null ? BigDecimal.ZERO.intValue() : scheduleOperLog.getBeforeDayOrder());

                // 操作后机台code转换为机台名称导出
                String afterMachineCode = scheduleOperLog.getAfterMachineCode();
                StringBuilder afterMachineName = new StringBuilder();
                if (StringUtil.isNotBlank(afterMachineCode)) {
                    String[] afterMachineCodeArr = afterMachineCode.split(",");
                    for (String machineCode : afterMachineCodeArr) {
                        afterMachineName.append(machineNameMap.getOrDefault(mixArea + machineCode, ""))
                                .append(",");
                    }
                }
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterMachineCode() == null ? "" : afterMachineName.substring(0, afterMachineName.length() - 1));
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterRecipeTypeName() == null ? "" : scheduleOperLog.getAfterRecipeTypeName());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterRecipeVersionId() == null ? "" : scheduleOperLog.getAfterRecipeVersionId());
                String afterRecipeStage = scheduleOperLog.getAfterRecipeStage();
                row.createCell(cellNum++).setCellValue(afterRecipeStage == null ? "" : recipeStageDictMap.getOrDefault(afterRecipeStage, ""));
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterMidPlan() == null ? BigDecimal.ZERO.doubleValue() : scheduleOperLog.getAfterMidPlan());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterMidOrder() == null ? BigDecimal.ZERO.intValue() : scheduleOperLog.getAfterMidOrder());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterNightPlan() == null ? BigDecimal.ZERO.doubleValue() : scheduleOperLog.getAfterNightPlan());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterNightOrder() == null ? BigDecimal.ZERO.intValue() : scheduleOperLog.getAfterNightOrder());
                row.createCell(cellNum++).setCellValue(scheduleOperLog.getAfterDayPlan() == null ? BigDecimal.ZERO.doubleValue() : scheduleOperLog.getAfterDayPlan());
                row.createCell(cellNum).setCellValue(scheduleOperLog.getAfterDayOrder() == null ? BigDecimal.ZERO.intValue() : scheduleOperLog.getAfterDayOrder());
                for (int j = 0; j <= cellNum; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
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

    /**
     * 将配方类型匹配后并赋值给配方类型名称
     * @param list 要匹配的配方类型集合
     */
    private void setRecipeTypeName(List<ScheduleOperLog> list) {
        List<RecipeType> recipeTypeList = scheduleOperLogMapper.selectRecipeTypeList(new RecipeType());
        Map<String, String> recipeTypeMap = recipeTypeList.stream().collect(Collectors.toMap(RecipeType::getRecipeTypeCode, RecipeType::getRecipeTypeName));
        for (ScheduleOperLog operLog : list) {
            String beforeRecipeType = operLog.getBeforeRecipeType();
            String beforeRecipeTypeName = recipeTypeMap.getOrDefault(beforeRecipeType, "");
            operLog.setBeforeRecipeTypeName(beforeRecipeTypeName);
            String afterRecipeType = operLog.getAfterRecipeType();
            String afterRecipeTypeName = recipeTypeMap.getOrDefault(afterRecipeType, "");
            operLog.setAfterRecipeTypeName(afterRecipeTypeName);
        }
    }
}
