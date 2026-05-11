package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.aps.lh.api.domain.entity.LhShiftConfig;
import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;
import com.zlt.aps.lh.mapper.CxLhScheduleResultMapper;
import com.zlt.aps.lh.mapper.CxScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhMouldCleanPlanMapper;
import com.zlt.aps.lh.mapper.LhShiftConfigMapper;
import com.zlt.aps.lh.service.IScheduleSummaryReportService;
import com.zlt.aps.constant.FactoryConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排产小结报表服务实现
 *
 * <p>聚合成型排程结果(T_CX_SCHEDULE_RESULT)和硫化排程结果(T_LH_SCHEDULE_RESULT)数据，
 * 结合班次配置(T_LH_SHIFT_CONFIG)、模具清洗计划(T_LH_MOULD_CLEAN_PLAN)等辅助表，
 * 使用Apache POI直接生成排产小结报表Excel。</p>
 *
 * <p>班次映射规则：T_LH_SHIFT_CONFIG 中 SHIFT_INDEX(1~8) 对应 CLASS1~CLASS8，
 * SHIFT_TYPE 标识班次类型（夜班/早班/中班），报表按三大班汇总。</p>
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ScheduleSummaryReportServiceImpl implements IScheduleSummaryReportService {

    @Resource
    private CxLhScheduleResultMapper cxLhScheduleResultMapper;

    @Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Resource
    private LhShiftConfigMapper lhShiftConfigMapper;

    @Resource
    private LhMouldCleanPlanMapper lhMouldCleanPlanMapper;

    @Override
    public byte[] exportScheduleSummaryReport(ScheduleSummaryReportVO queryVO) {
        if (queryVO == null || StringUtils.isBlank(queryVO.getScheduleDate())) {
            throw new ServiceException("排程日期不能为空");
        }

        Date scheduleDate = DateUtil.parse(queryVO.getScheduleDate(), "yyyy-MM-dd");
        String factoryCode = StringUtils.defaultString(queryVO.getFactoryCode(), FactoryConstant.DEFAULT_FACTORY_CODE);

        List<LhShiftConfig> shiftConfigs = loadShiftConfigs(factoryCode);
        Map<Integer, String> classShiftTypeMap = buildClassShiftTypeMap(shiftConfigs);
        Map<String, String> shiftTypeMap = buildShiftTypeMapping(shiftConfigs);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("排产小结");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);

            int rowIdx = 0;

            rowIdx = writeTitle(sheet, rowIdx, scheduleDate, titleStyle);
            rowIdx = writeCxSection(sheet, rowIdx, scheduleDate, factoryCode, classShiftTypeMap, shiftTypeMap, headerStyle, dataStyle, sectionStyle);
            rowIdx = writeLhSection(sheet, rowIdx, scheduleDate, factoryCode, classShiftTypeMap, shiftTypeMap, headerStyle, dataStyle, sectionStyle);
            rowIdx = writeMouldSection(sheet, rowIdx, scheduleDate, factoryCode, headerStyle, dataStyle, sectionStyle);
            rowIdx = writeRemarkSection(sheet, rowIdx, scheduleDate, factoryCode, headerStyle, dataStyle, sectionStyle);

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(0, 3000);
            sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 4000);
            sheet.setColumnWidth(4, 4000);
            sheet.setColumnWidth(5, 6000);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("生成排产小结报表失败", e);
            throw new ServiceException("生成排产小结报表失败：" + e.getMessage());
        }
    }

    /**
     * 写入标题行
     */
    private int writeTitle(Sheet sheet, int rowIdx, Date scheduleDate, CellStyle titleStyle) {
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("排产小结 " + DateUtil.format(scheduleDate, "MM月dd日"));
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        return rowIdx;
    }

    /**
     * 写入成型区域
     */
    private int writeCxSection(Sheet sheet, int rowIdx, Date scheduleDate, String factoryCode,
                                Map<Integer, String> classShiftTypeMap, Map<String, String> shiftTypeMap,
                                CellStyle headerStyle, CellStyle dataStyle, CellStyle sectionStyle) {
        Row sectionRow = sheet.createRow(rowIdx++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue("成型");
        sectionCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));

        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"项目", shiftTypeMap.getOrDefault("夜班", "夜班"),
                shiftTypeMap.getOrDefault("早班", "早班"),
                shiftTypeMap.getOrDefault("中班", "中班"), "合计", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode)
                        .orderByAsc(CxScheduleResult::getCxMachineCode));

        BigDecimal cxNightTotal = BigDecimal.ZERO;
        BigDecimal cxMorningTotal = BigDecimal.ZERO;
        BigDecimal cxMiddleTotal = BigDecimal.ZERO;

        String prevStructure = null;
        Set<String> structureChanges = new LinkedHashSet<>();

        for (CxScheduleResult result : cxResults) {
            BigDecimal nightQty = sumCxQtyByShiftType(result, classShiftTypeMap, "夜班");
            BigDecimal morningQty = sumCxQtyByShiftType(result, classShiftTypeMap, "早班");
            BigDecimal middleQty = sumCxQtyByShiftType(result, classShiftTypeMap, "中班");

            cxNightTotal = cxNightTotal.add(nightQty);
            cxMorningTotal = cxMorningTotal.add(morningQty);
            cxMiddleTotal = cxMiddleTotal.add(middleQty);

            String currentStructure = StringUtils.defaultString(result.getStructureName()).trim();
            if (prevStructure != null && !prevStructure.equals(currentStructure)) {
                structureChanges.add(prevStructure + "→" + currentStructure);
            }
            prevStructure = currentStructure;
        }

        Row cxRow = sheet.createRow(rowIdx++);
        writeDataRow(cxRow, new Object[]{"成型产量", cxNightTotal, cxMorningTotal, cxMiddleTotal,
                cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal), ""}, dataStyle);

        Row switchRow = sheet.createRow(rowIdx++);
        writeDataRow(switchRow, new Object[]{"规格切换", "", "", "",
                String.join("；", structureChanges), ""}, dataStyle);

        return rowIdx;
    }

    /**
     * 写入硫化区域
     */
    private int writeLhSection(Sheet sheet, int rowIdx, Date scheduleDate, String factoryCode,
                                Map<Integer, String> classShiftTypeMap, Map<String, String> shiftTypeMap,
                                CellStyle headerStyle, CellStyle dataStyle, CellStyle sectionStyle) {
        Row sectionRow = sheet.createRow(rowIdx++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue("硫化");
        sectionCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));

        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"项目", shiftTypeMap.getOrDefault("夜班", "夜班"),
                shiftTypeMap.getOrDefault("早班", "早班"),
                shiftTypeMap.getOrDefault("中班", "中班"), "合计", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults = cxLhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult>()
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getScheduleDate, scheduleDate)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getFactoryCode, factoryCode)
                        .orderByAsc(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getLhMachineCode));

        BigDecimal lhNightTotal = BigDecimal.ZERO;
        BigDecimal lhMorningTotal = BigDecimal.ZERO;
        BigDecimal lhMiddleTotal = BigDecimal.ZERO;

        for (com.zlt.aps.cx.entity.schedule.LhScheduleResult result : lhResults) {
            lhNightTotal = lhNightTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "夜班"));
            lhMorningTotal = lhMorningTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "早班"));
            lhMiddleTotal = lhMiddleTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "中班"));
        }

        Row lhRow = sheet.createRow(rowIdx++);
        writeDataRow(lhRow, new Object[]{"硫化产量", lhNightTotal, lhMorningTotal, lhMiddleTotal,
                lhNightTotal.add(lhMorningTotal).add(lhMiddleTotal), ""}, dataStyle);

        long nightMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "夜班");
        long morningMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "早班");
        long middleMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "中班");

        Row machineRow = sheet.createRow(rowIdx++);
        writeDataRow(machineRow, new Object[]{"硫化开动", nightMachines, morningMachines, middleMachines, "", ""}, dataStyle);

        return rowIdx;
    }

    /**
     * 写入模具区域
     */
    private int writeMouldSection(Sheet sheet, int rowIdx, Date scheduleDate, String factoryCode,
                                   CellStyle headerStyle, CellStyle dataStyle, CellStyle sectionStyle) {
        Row sectionRow = sheet.createRow(rowIdx++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue("模具");
        sectionCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));

        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"项目", "内容", "", "", "", ""};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Row mouldRow = sheet.createRow(rowIdx++);
        writeDataRow(mouldRow, new Object[]{"模具交管", "", "", "", "", ""}, dataStyle);

        LocalDate localDate = scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Date startDate = Date.from(localDate.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(localDate.plusDays(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        List<LhMouldCleanPlan> cleanPlans = lhMouldCleanPlanMapper.selectList(
                new LambdaQueryWrapper<LhMouldCleanPlan>()
                        .eq(LhMouldCleanPlan::getFactoryCode, factoryCode)
                        .ge(LhMouldCleanPlan::getCleanTime, startDate)
                        .le(LhMouldCleanPlan::getCleanTime, endDate));

        String cleanInfo = "";
        if (!cleanPlans.isEmpty()) {
            String cleanDateStr = DateUtil.format(cleanPlans.get(0).getCleanTime(), "MM月dd日");
            String mouldCodes = cleanPlans.stream()
                    .map(LhMouldCleanPlan::getLhCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("、"));
            cleanInfo = cleanDateStr + " " + mouldCodes;
        }

        Row cleanRow = sheet.createRow(rowIdx++);
        writeDataRow(cleanRow, new Object[]{"模具清洗", cleanInfo, "", "", "", ""}, dataStyle);

        return rowIdx;
    }

    /**
     * 写入备注区域
     */
    private int writeRemarkSection(Sheet sheet, int rowIdx, Date scheduleDate, String factoryCode,
                                    CellStyle headerStyle, CellStyle dataStyle, CellStyle sectionStyle) {
        Row sectionRow = sheet.createRow(rowIdx++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue("备注");
        sectionCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));

        String cxRemark = buildCxRemark(scheduleDate, factoryCode);
        String lhRemark = buildLhRemark(scheduleDate, factoryCode);

        Row cxRemarkRow = sheet.createRow(rowIdx++);
        writeDataRow(cxRemarkRow, new Object[]{"成型备注", cxRemark, "", "", "", ""}, dataStyle);

        Row lhRemarkRow = sheet.createRow(rowIdx++);
        writeDataRow(lhRemarkRow, new Object[]{"硫化备注", lhRemark, "", "", "", ""}, dataStyle);

        return rowIdx;
    }

    /**
     * 写入一行数据
     */
    private void writeDataRow(Row row, Object[] values, CellStyle dataStyle) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (values[i] instanceof BigDecimal) {
                cell.setCellValue(((BigDecimal) values[i]).doubleValue());
            } else if (values[i] instanceof Long) {
                cell.setCellValue((Long) values[i]);
            } else if (values[i] instanceof Integer) {
                cell.setCellValue((Integer) values[i]);
            } else if (values[i] instanceof Number) {
                cell.setCellValue(((Number) values[i]).doubleValue());
            } else {
                cell.setCellValue(StringUtils.defaultString(String.valueOf(values[i])));
            }
            cell.setCellStyle(dataStyle);
        }
    }

    /**
     * 创建标题样式
     */
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建区域标题样式
     */
    private CellStyle createSectionStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * 构建成型备注信息
     */
    private String buildCxRemark(Date scheduleDate, String factoryCode) {
        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));

        return cxResults.stream()
                .map(CxScheduleResult::getSpecialRequirements)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    /**
     * 构建硫化备注信息
     */
    private String buildLhRemark(Date scheduleDate, String factoryCode) {
        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults = cxLhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult>()
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getScheduleDate, scheduleDate)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getFactoryCode, factoryCode));

        return lhResults.stream()
                .map(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getRemark)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    /**
     * 加载班次配置列表
     */
    private List<LhShiftConfig> loadShiftConfigs(String factoryCode) {
        return lhShiftConfigMapper.selectList(
                new LambdaQueryWrapper<LhShiftConfig>()
                        .eq(LhShiftConfig::getFactoryCode, factoryCode)
                        .orderByAsc(LhShiftConfig::getShiftIndex));
    }

    /**
     * 构建班次类型映射：班次序号 → 班次类型（早班/中班/夜班）
     */
    private Map<Integer, String> buildClassShiftTypeMap(List<LhShiftConfig> shiftConfigs) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (LhShiftConfig config : shiftConfigs) {
            if (config.getShiftIndex() != null && StringUtils.isNotBlank(config.getShiftType())) {
                map.put(config.getShiftIndex(), config.getShiftType());
            }
        }
        return map;
    }

    /**
     * 构建班次类型名称映射，用于表头
     */
    private Map<String, String> buildShiftTypeMapping(List<LhShiftConfig> shiftConfigs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (LhShiftConfig config : shiftConfigs) {
            if (StringUtils.isNotBlank(config.getShiftType()) && StringUtils.isNotBlank(config.getShiftName())) {
                map.putIfAbsent(config.getShiftType(), config.getShiftName());
            }
        }
        return map;
    }

    /**
     * 按班次类型汇总成型排程结果的计划量
     */
    private BigDecimal sumCxQtyByShiftType(CxScheduleResult result,
                                            Map<Integer, String> classShiftTypeMap,
                                            String shiftType) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
            if (shiftType.equals(entry.getValue())) {
                BigDecimal qty = getCxClassPlanQty(result, entry.getKey());
                total = total.add(qty);
            }
        }
        return total;
    }

    /**
     * 获取成型排程结果指定班次的计划量
     */
    private BigDecimal getCxClassPlanQty(CxScheduleResult result, int classIndex) {
        switch (classIndex) {
            case 1: return result.getClass1PlanQty() != null ? result.getClass1PlanQty() : BigDecimal.ZERO;
            case 2: return result.getClass2PlanQty() != null ? result.getClass2PlanQty() : BigDecimal.ZERO;
            case 3: return result.getClass3PlanQty() != null ? result.getClass3PlanQty() : BigDecimal.ZERO;
            case 4: return result.getClass4PlanQty() != null ? result.getClass4PlanQty() : BigDecimal.ZERO;
            case 5: return result.getClass5PlanQty() != null ? result.getClass5PlanQty() : BigDecimal.ZERO;
            case 6: return result.getClass6PlanQty() != null ? result.getClass6PlanQty() : BigDecimal.ZERO;
            case 7: return result.getClass7PlanQty() != null ? result.getClass7PlanQty() : BigDecimal.ZERO;
            case 8: return result.getClass8PlanQty() != null ? result.getClass8PlanQty() : BigDecimal.ZERO;
            default: return BigDecimal.ZERO;
        }
    }

    /**
     * 按班次类型汇总硫化排程结果的计划量
     */
    private BigDecimal sumLhQtyByShiftType(com.zlt.aps.cx.entity.schedule.LhScheduleResult result,
                                            Map<Integer, String> classShiftTypeMap,
                                            String shiftType) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
            if (shiftType.equals(entry.getValue())) {
                BigDecimal qty = getLhClassPlanQty(result, entry.getKey());
                total = total.add(qty);
            }
        }
        return total;
    }

    /**
     * 获取硫化排程结果指定班次的计划量
     */
    private BigDecimal getLhClassPlanQty(com.zlt.aps.cx.entity.schedule.LhScheduleResult result, int classIndex) {
        Integer qty = null;
        switch (classIndex) {
            case 1: qty = result.getClass1PlanQty(); break;
            case 2: qty = result.getClass2PlanQty(); break;
            case 3: qty = result.getClass3PlanQty(); break;
            case 4: qty = result.getClass4PlanQty(); break;
            case 5: qty = result.getClass5PlanQty(); break;
            case 6: qty = result.getClass6PlanQty(); break;
            case 7: qty = result.getClass7PlanQty(); break;
            case 8: qty = result.getClass8PlanQty(); break;
            default: break;
        }
        return qty != null ? BigDecimal.valueOf(qty) : BigDecimal.ZERO;
    }

    /**
     * 按班次类型统计硫化开动机台数
     */
    private long countLhMachinesByShiftType(List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults,
                                             Map<Integer, String> classShiftTypeMap,
                                             String shiftType) {
        Set<String> machineCodes = new HashSet<>();
        for (com.zlt.aps.cx.entity.schedule.LhScheduleResult result : lhResults) {
            for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
                if (shiftType.equals(entry.getValue())) {
                    BigDecimal qty = getLhClassPlanQty(result, entry.getKey());
                    if (qty.compareTo(BigDecimal.ZERO) > 0) {
                        machineCodes.add(result.getLhMachineCode());
                    }
                }
            }
        }
        return machineCodes.size();
    }
}
