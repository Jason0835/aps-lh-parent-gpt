package com.zlt.aps.mp.report.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.mp.api.domain.dto.MonthPlanCompareDto;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.vo.MonthPlanCompareVo;
import com.zlt.aps.mp.report.mapper.MonthPlanCompareMapper;
import com.zlt.aps.mp.report.service.IMonthPlanCompareService;
import com.zlt.common.utils.ExcelReadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 月计划与实际产量对比报表服务实现
 *
 * @author APS
 * @date 2026-08-13
 */
@Slf4j
@Service
public class MonthPlanCompareServiceImpl extends BaseController implements IMonthPlanCompareService {

    /**
     * 行类型常量：月计划
     */
    private static final String ROW_TYPE_PLAN = "plan";

    /**
     * 行类型常量：实际产量
     */
    private static final String ROW_TYPE_ACTUAL = "actual";

    /**
     * 行类型常量：差异
     */
    private static final String ROW_TYPE_DIFF = "diff";

    /**
     * 行类型常量：完成率
     */
    private static final String ROW_TYPE_RATE = "rate";

    /**
     * 完成率红色阈值（小于60%）
     */
    private static final BigDecimal RATE_RED_THRESHOLD = new BigDecimal("60");

    /**
     * 完成率黄色阈值（60%~80%）
     */
    private static final BigDecimal RATE_YELLOW_THRESHOLD = new BigDecimal("80");

    @Autowired
    private MonthPlanCompareMapper monthPlanCompareMapper;

    /**
     * 默认每页SKU数量
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 查询月计划与实际产量对比列表（全量，用于导出）
     * <p>每个SKU返回4行（月计划/实际产量/差异/完成率）</p>
     * <p>清除分页参数后查询全量数据</p>
     *
     * @param queryDto 查询参数
     * @return 结果列表
     */
    @Override
    public List<MonthPlanCompareVo> listMonthPlanCompare(MonthPlanCompareDto queryDto) {
        // 清除分页参数，确保按全量查询
        queryDto.setPageNum(null);
        queryDto.setPageSize(null);
        queryDto.setOffset(null);
        queryDto.setMaterialKeys(null);

        // 1. 查询定稿主数据列表（全量）
        List<FactoryMonthPlanProductionFinalResult> finalList = monthPlanCompareMapper.selectFinalList(queryDto);
        if (CollectionUtils.isEmpty(finalList)) {
            return Collections.emptyList();
        }

        // 2. 查询每日实际完成量（全量），按 materialCode|lhType 分组，内层按 dayNum 分组
        List<Map<String, Object>> dailyFinishList = monthPlanCompareMapper.selectDailyFinishQtyList(queryDto);

        // 3. 计算当月天数
        YearMonth yearMonth = YearMonth.of(queryDto.getYear(), queryDto.getMonth());
        int daysInMonth = yearMonth.lengthOfMonth();

        // 4. 组装结果
        return this.buildVoList(finalList, dailyFinishList, daysInMonth);
    }

    /**
     * 查询月计划与实际产量对比列表（分页，用于列表展示）
     * <p>按 SKU 分页，total 为 SKU 总数，rows 为当前页 SKU 的 4 行 VO</p>
     *
     * @param queryDto 查询参数（需包含 pageNum/pageSize）
     * @return 分页结果（total=SKU总数，rows=当前页4×N行VO）
     */
    @Override
    public TableDataInfo listMonthPlanComparePage(MonthPlanCompareDto queryDto) {
        // 1. 校验并补全分页参数
        Integer pageNum = queryDto.getPageNum();
        Integer pageSize = queryDto.getPageSize();
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
            queryDto.setPageNum(pageNum);
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
            queryDto.setPageSize(pageSize);
        }
        queryDto.setOffset((pageNum - 1) * pageSize);

        // 2. 查询当前条件下 SKU 总数
        int total = monthPlanCompareMapper.selectFinalListCount(queryDto);
        if (total <= 0) {
            return this.buildTableDataInfo(Collections.emptyList(), 0);
        }

        // 3. 分页查询当前页 SKU 定稿主数据
        List<FactoryMonthPlanProductionFinalResult> finalList = monthPlanCompareMapper.selectFinalList(queryDto);
        if (CollectionUtils.isEmpty(finalList)) {
            return this.buildTableDataInfo(Collections.emptyList(), total);
        }

        // 4. 提取当前页 SKU 的物料键列表（materialCode|productStatus），用于限定实际产量查询范围
        List<String> materialKeys = finalList.stream()
                .map(item -> item.getMaterialCode() + "|" + StringUtils.trimToEmpty(item.getProductStatus()))
                .collect(Collectors.toList());
        queryDto.setMaterialKeys(materialKeys);

        // 5. 查询当前页 SKU 的每日实际完成量
        List<Map<String, Object>> dailyFinishList = monthPlanCompareMapper.selectDailyFinishQtyList(queryDto);

        // 6. 计算当月天数
        YearMonth yearMonth = YearMonth.of(queryDto.getYear(), queryDto.getMonth());
        int daysInMonth = yearMonth.lengthOfMonth();

        // 7. 组装当前页 VO 列表
        List<MonthPlanCompareVo> voList = this.buildVoList(finalList, dailyFinishList, daysInMonth);

        // 8. 返回分页结果（total=SKU总数，rows=当前页4×N行VO）
        return this.buildTableDataInfo(voList, total);
    }

    /**
     * 构建分页结果 TableDataInfo
     * <p>显式设置 code=200 和 msg，避免前端响应拦截器将默认 code 当作错误处理</p>
     *
     * @param rows  当前页数据
     * @param total 总数
     * @return TableDataInfo
     */
    private TableDataInfo buildTableDataInfo(List<?> rows, long total) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(rows);
        rspData.setMsg("查询成功");
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 组装 VO 列表（每个 SKU 扩展为 4 行：月计划/实际产量/差异/完成率）
     *
     * @param finalList       定稿主数据列表
     * @param dailyFinishList 每日实际完成量列表
     * @param daysInMonth     当月天数
     * @return VO 列表
     */
    private List<MonthPlanCompareVo> buildVoList(List<FactoryMonthPlanProductionFinalResult> finalList,
                                                  List<Map<String, Object>> dailyFinishList, int daysInMonth) {
        Map<String, Map<Integer, BigDecimal>> actualMap = this.buildActualMap(dailyFinishList);
        List<MonthPlanCompareVo> result = new ArrayList<>(finalList.size() * 4);
        for (FactoryMonthPlanProductionFinalResult item : finalList) {
            String key = item.getMaterialCode() + "|" + StringUtils.trimToEmpty(item.getProductStatus());
            Map<Integer, BigDecimal> dailyActual = actualMap.getOrDefault(key, Collections.emptyMap());

            // 构建日计划数组、日实际数组
            List<BigDecimal> planDays = new ArrayList<>(daysInMonth);
            List<BigDecimal> actualDays = new ArrayList<>(daysInMonth);
            BigDecimal planTotal = BigDecimal.ZERO;
            BigDecimal actualTotal = BigDecimal.ZERO;
            for (int day = 1; day <= daysInMonth; day++) {
                BigDecimal planQty = BigDecimalUtils.valueOf(this.getDayFieldValue(item, day));
                BigDecimal actQty = dailyActual.getOrDefault(day, BigDecimal.ZERO);
                planDays.add(planQty);
                actualDays.add(actQty);
                planTotal = planTotal.add(planQty);
                actualTotal = actualTotal.add(actQty);
            }

            // 月计划行
            result.add(this.buildVo(item, ROW_TYPE_PLAN, planTotal, planDays));
            // 实际产量行
            result.add(this.buildVo(item, ROW_TYPE_ACTUAL, actualTotal, actualDays));
            // 差异行 = 实际 - 计划
            result.add(this.buildDiffVo(item, planDays, actualDays, daysInMonth));
            // 完成率行 = 实际 / 计划（百分比）
            result.add(this.buildRateVo(item, planDays, actualDays, planTotal, actualTotal, daysInMonth));
        }
        return result;
    }

    /**
     * 导出月计划与实际产量对比数据
     *
     * @param queryDto 查询参数
     * @return Excel 文件字节数组
     */
    @Override
    public byte[] exportMonthPlanCompare(MonthPlanCompareDto queryDto) {
        List<MonthPlanCompareVo> dataList = this.listMonthPlanCompare(queryDto);
        int daysInMonth = YearMonth.of(queryDto.getYear(), queryDto.getMonth()).lengthOfMonth();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("月计划与实际产量对比");

            // 1. 创建样式
            CellStyle headerStyle = this.createHeaderStyle(workbook);
            CellStyle normalStyle = this.createNormalStyle(workbook);
            CellStyle textCenterStyle = this.createTextCenterStyle(workbook);
            CellStyle textLeftStyle = this.createTextLeftStyle(workbook);
            CellStyle redStyle = this.createColorStyle(workbook, new byte[]{(byte) 0xFD, (byte) 0xE2, (byte) 0xE2}, new byte[]{(byte) 0xF5, (byte) 0x6C, (byte) 0x6C});
            CellStyle yellowStyle = this.createColorStyle(workbook, new byte[]{(byte) 0xFA, (byte) 0xEC, (byte) 0xD8}, new byte[]{(byte) 0xE6, (byte) 0xA2, (byte) 0x3C});

            // 2. 写表头行
            this.writeHeaderRow(sheet, headerStyle, daysInMonth, queryDto.getMonth());

            // 3. 写数据行（每SKU 4行）
            int rowIdx = 1;
            for (int i = 0; i < dataList.size(); i += 4) {
                // 写4行（plan/actual/diff/rate）
                for (int r = 0; r < 4; r++) {
                    MonthPlanCompareVo vo = dataList.get(i + r);
                    Row dataRow = sheet.createRow(rowIdx + r);

                    // A列：物料编码（每行都创建单元格并设置边框，内容只在第一行写入）
                    Cell codeCell = dataRow.createCell(0);
                    codeCell.setCellStyle(textCenterStyle);
                    // B列：物料描述（每行都创建单元格并设置边框，内容只在第一行写入）
                    Cell descCell = dataRow.createCell(1);
                    descCell.setCellStyle(textLeftStyle);

                    // C列：行类型标签
                    Cell typeCell = dataRow.createCell(2);
                    typeCell.setCellValue(vo.getRowTypeLabel());
                    typeCell.setCellStyle(textCenterStyle);
                    // D列：合计
                    Cell totalCell = dataRow.createCell(3);
                    if (ROW_TYPE_RATE.equals(vo.getRowType())) {
                        // 完成率合计特殊处理
                        this.setRateCell(totalCell, vo.getTotalQty(), normalStyle, redStyle, yellowStyle);
                    } else {
                        this.setNumericCell(totalCell, vo.getTotalQty(), normalStyle);
                    }
                    // E列起：每日数据
                    for (int day = 0; day < daysInMonth; day++) {
                        Cell dayCell = dataRow.createCell(4 + day);
                        BigDecimal dayValue = vo.getDayQtyList().get(day);
                        if (ROW_TYPE_RATE.equals(vo.getRowType())) {
                            this.setRateCell(dayCell, dayValue, normalStyle, redStyle, yellowStyle);
                        } else {
                            this.setNumericCell(dayCell, dayValue, normalStyle);
                        }
                    }
                }
                // 第一行写入A/B列内容（合并后只显示第一行内容）
                Row firstRow = sheet.getRow(rowIdx);
                firstRow.getCell(0).setCellValue(dataList.get(i).getMaterialCode());
                if (dataList.get(i).getMaterialDesc() != null) {
                    firstRow.getCell(1).setCellValue(dataList.get(i).getMaterialDesc());
                }
                // 合并A列（物料编码）和B列（物料描述）4行
                // 注意：必须先创建所有单元格并设置边框，再合并，否则边框不显示
                CellRangeAddress codeRegion = new CellRangeAddress(rowIdx, rowIdx + 3, 0, 0);
                sheet.addMergedRegion(codeRegion);
                CellRangeAddress descRegion = new CellRangeAddress(rowIdx, rowIdx + 3, 1, 1);
                sheet.addMergedRegion(descRegion);
                // 使用RegionUtil为合并区域设置边框，确保四边边框完整
                this.setRegionBorder(codeRegion, sheet, BorderStyle.THIN);
                this.setRegionBorder(descRegion, sheet, BorderStyle.THIN);
                rowIdx += 4;
            }

            // 4. 写底部备注（颜色图例）
            this.writeLegendRows(sheet, rowIdx, textLeftStyle, daysInMonth);

            // 5. 设置列宽
            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 6000);
            sheet.setColumnWidth(2, 3000);
            sheet.setColumnWidth(3, 3000);
            for (int day = 0; day < daysInMonth; day++) {
                sheet.setColumnWidth(4 + day, 2200);
            }

            // 6. 输出字节数组
            return ExcelReadUtils.writeExcel(workbook);
        } catch (IOException e) {
            log.error("导出月计划与实际产量对比数据失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 构建每日实际完成量分组Map
     * <p>外层key: materialCode|lhType, 内层key: dayNum, value: 完成量</p>
     *
     * @param dailyFinishList 查询结果
     * @return 分组Map
     */
    private Map<String, Map<Integer, BigDecimal>> buildActualMap(List<Map<String, Object>> dailyFinishList) {
        if (CollectionUtils.isEmpty(dailyFinishList)) {
            return Collections.emptyMap();
        }
        Map<String, Map<Integer, BigDecimal>> result = new HashMap<>(dailyFinishList.size());
        for (Map<String, Object> row : dailyFinishList) {
            String materialCode = String.valueOf(row.get("materialCode"));
            String lhType = String.valueOf(row.get("lhType"));
            Integer dayNum = ((Number) row.get("dayNum")).intValue();
            BigDecimal finishQty = BigDecimalUtils.valueOf(row.get("finishQty"));
            String key = materialCode + "|" + StringUtils.trimToEmpty(lhType);
            result.computeIfAbsent(key, k -> new HashMap<>(32)).put(dayNum, finishQty);
        }
        return result;
    }

    /**
     * 通过反射动态获取定稿实体的 DAY_n 字段值
     *
     * @param entity 定稿实体
     * @param day    日期（1~31）
     * @return 计划量
     */
    private Integer getDayFieldValue(FactoryMonthPlanProductionFinalResult entity, int day) {
        try {
            String fieldName = String.format("day%d", day);
            java.lang.reflect.Field field = FactoryMonthPlanProductionFinalResult.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value == null) {
                return 0;
            }
            return (Integer) value;
        } catch (Exception e) {
            log.warn("获取 DAY_{} 字段值失败", day, e);
            return 0;
        }
    }

    /**
     * 构建月计划/实际产量行VO
     *
     * @param item     定稿实体
     * @param rowType  行类型
     * @param totalQty 合计值
     * @param dayList  每日数据
     * @return VO
     */
    private MonthPlanCompareVo buildVo(FactoryMonthPlanProductionFinalResult item, String rowType,
                                       BigDecimal totalQty, List<BigDecimal> dayList) {
        MonthPlanCompareVo vo = new MonthPlanCompareVo();
        vo.setMaterialCode(item.getMaterialCode());
        vo.setMaterialDesc(item.getMaterialDesc());
        vo.setRowType(rowType);
        vo.setRowTypeLabel(this.getRowTypeLabel(rowType));
        vo.setTotalQty(totalQty);
        vo.setDayQtyList(dayList);
        return vo;
    }

    /**
     * 构建差异行VO（实际 - 计划）
     *
     * @param item       定稿实体
     * @param planDays   日计划数组
     * @param actualDays 日实际数组
     * @param daysInMonth 当月天数
     * @return VO
     */
    private MonthPlanCompareVo buildDiffVo(FactoryMonthPlanProductionFinalResult item,
                                           List<BigDecimal> planDays, List<BigDecimal> actualDays, int daysInMonth) {
        List<BigDecimal> diffDays = new ArrayList<>(daysInMonth);
        BigDecimal diffTotal = BigDecimal.ZERO;
        for (int day = 0; day < daysInMonth; day++) {
            BigDecimal diff = actualDays.get(day).subtract(planDays.get(day));
            diffDays.add(diff);
            diffTotal = diffTotal.add(diff);
        }
        return this.buildVo(item, ROW_TYPE_DIFF, diffTotal, diffDays);
    }

    /**
     * 构建完成率行VO（实际 / 计划 * 100，百分比）
     * <p>分母为0时返回null（前端展示为"-"）</p>
     *
     * @param item        定稿实体
     * @param planDays    日计划数组
     * @param actualDays  日实际数组
     * @param planTotal   计划合计
     * @param actualTotal 实际合计
     * @param daysInMonth 当月天数
     * @return VO
     */
    private MonthPlanCompareVo buildRateVo(FactoryMonthPlanProductionFinalResult item,
                                           List<BigDecimal> planDays, List<BigDecimal> actualDays,
                                           BigDecimal planTotal, BigDecimal actualTotal, int daysInMonth) {
        List<BigDecimal> rateDays = new ArrayList<>(daysInMonth);
        for (int day = 0; day < daysInMonth; day++) {
            BigDecimal planQty = planDays.get(day);
            BigDecimal actQty = actualDays.get(day);
            BigDecimal rate = this.calcRate(actQty, planQty);
            rateDays.add(rate);
        }
        // 合计完成率 = 实际合计 / 计划合计
        BigDecimal rateTotal = this.calcRate(actualTotal, planTotal);
        return this.buildVo(item, ROW_TYPE_RATE, rateTotal, rateDays);
    }

    /**
     * 计算完成率（百分比）
     * <p>分母为0或null时返回null</p>
     *
     * @param actual 实际值
     * @param plan   计划值
     * @return 完成率百分比（如 75.50），分母为0返回null
     */
    private BigDecimal calcRate(BigDecimal actual, BigDecimal plan) {
        if (plan == null || plan.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (actual == null) {
            actual = BigDecimal.ZERO;
        }
        return actual.multiply(new BigDecimal("100"))
                .divide(plan, 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取行类型标签
     *
     * @param rowType 行类型
     * @return 标签
     */
    private String getRowTypeLabel(String rowType) {
        switch (rowType) {
            case ROW_TYPE_PLAN:
                return "月计划";
            case ROW_TYPE_ACTUAL:
                return "实际产量";
            case ROW_TYPE_DIFF:
                return "差异";
            case ROW_TYPE_RATE:
                return "完成率";
            default:
                return StringUtils.EMPTY;
        }
    }

    /**
     * 创建表头样式
     *
     * @param workbook 工作簿
     * @return 样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建普通数据样式（数值右对齐，用于合计列和日期列）
     *
     * @param workbook 工作簿
     * @return 样式
     */
    private CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    /**
     * 创建文本居中样式（用于物料编码、行类型标签等文本列）
     *
     * @param workbook 工作簿
     * @return 样式
     */
    private CellStyle createTextCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建文本居左样式（用于物料描述等长文本列）
     *
     * @param workbook 工作簿
     * @return 样式
     */
    private CellStyle createTextLeftStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建颜色样式（用于完成率着色）
     *
     * @param workbook    工作簿
     * @param bgRgb       背景RGB
     * @param fontRgb     字体RGB
     * @return 样式
     */
    private CellStyle createColorStyle(Workbook workbook, byte[] bgRgb, byte[] fontRgb) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        if (workbook instanceof XSSFWorkbook) {
            org.apache.poi.xssf.usermodel.XSSFCellStyle xssfStyle = (org.apache.poi.xssf.usermodel.XSSFCellStyle) style;
            org.apache.poi.xssf.usermodel.XSSFColor bgColor = new org.apache.poi.xssf.usermodel.XSSFColor(bgRgb, null);
            xssfStyle.setFillForegroundColor(bgColor);
            org.apache.poi.xssf.usermodel.XSSFColor fontColor = new org.apache.poi.xssf.usermodel.XSSFColor(fontRgb, null);
            org.apache.poi.xssf.usermodel.XSSFFont font = ((XSSFWorkbook) workbook).createFont();
            font.setColor(fontColor);
            font.setBold(true);
            xssfStyle.setFont(font);
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00\"%\""));
        return style;
    }

    /**
     * 写表头行
     *
     * @param sheet      工作表
     * @param headerStyle 表头样式
     * @param daysInMonth 当月天数
     * @param month      月份
     */
    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle, int daysInMonth, Integer month) {
        Row headerRow = sheet.createRow(0);
        // A列：物料编码
        Cell codeCell = headerRow.createCell(0);
        codeCell.setCellValue("物料编码");
        codeCell.setCellStyle(headerStyle);
        // B列：物料描述
        Cell descCell = headerRow.createCell(1);
        descCell.setCellValue("物料描述");
        descCell.setCellStyle(headerStyle);
        // C列：日期（行类型）
        Cell typeCell = headerRow.createCell(2);
        typeCell.setCellValue("日期");
        typeCell.setCellStyle(headerStyle);
        // D列：合计
        Cell totalCell = headerRow.createCell(3);
        totalCell.setCellValue("合计");
        totalCell.setCellStyle(headerStyle);
        // E列起：每日日期
        for (int day = 1; day <= daysInMonth; day++) {
            Cell dayCell = headerRow.createCell(3 + day);
            dayCell.setCellValue(month + "月" + day + "日");
            dayCell.setCellStyle(headerStyle);
        }
    }

    /**
     * 设置数值单元格
     *
     * @param cell  单元格
     * @param value 值
     * @param style 样式
     */
    private void setNumericCell(Cell cell, BigDecimal value, CellStyle style) {
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    /**
     * 设置完成率单元格（含条件着色）
     * <p>值null时显示"-"，<60%红色，60~80%黄色，>=80%默认</p>
     *
     * @param cell        单元格
     * @param rateValue   完成率值（百分比）
     * @param normalStyle 默认样式
     * @param redStyle    红色样式
     * @param yellowStyle 黄色样式
     */
    private void setRateCell(Cell cell, BigDecimal rateValue, CellStyle normalStyle,
                             CellStyle redStyle, CellStyle yellowStyle) {
        if (rateValue == null) {
            cell.setCellStyle(normalStyle);
            cell.setCellValue("-");
            return;
        }
        if (rateValue.compareTo(RATE_RED_THRESHOLD) < 0) {
            cell.setCellStyle(redStyle);
        } else if (rateValue.compareTo(RATE_YELLOW_THRESHOLD) < 0) {
            cell.setCellStyle(yellowStyle);
        } else {
            cell.setCellStyle(normalStyle);
        }
        cell.setCellValue(rateValue.doubleValue());
    }

    /**
     * 写底部备注行（颜色图例说明）
     *
     * @param sheet         工作表
     * @param rowIdx        起始行索引
     * @param textLeftStyle 文本左对齐样式
     * @param daysInMonth   当月天数
     */
    private void writeLegendRows(Sheet sheet, int rowIdx, CellStyle textLeftStyle, int daysInMonth) {
        // 空一行
        int legendRow = rowIdx + 1;
        Row noteRow = sheet.createRow(legendRow);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue("备注：差异 = 实际 - 计划；完成率 = 实际 / 计划 × 100%");
        noteCell.setCellStyle(textLeftStyle);
        sheet.addMergedRegion(new CellRangeAddress(legendRow, legendRow, 0, 3 + daysInMonth));

        // 颜色图例行
        Row legendRowData = sheet.createRow(legendRow + 1);
        Cell redCell = legendRowData.createCell(0);
        redCell.setCellValue("完成率 < 60%");
        redCell.setCellStyle(textLeftStyle);
        Cell yellowCell = legendRowData.createCell(1);
        yellowCell.setCellValue("60% ≤ 完成率 < 80%");
        yellowCell.setCellStyle(textLeftStyle);
    }

    /**
     * 为合并区域设置四边边框（解决POI合并单元格后边框缺失问题）
     *
     * @param region      合并区域
     * @param sheet       工作表
     * @param borderStyle 边框样式
     */
    private void setRegionBorder(CellRangeAddress region, Sheet sheet, BorderStyle borderStyle) {
        RegionUtil.setBorderTop(borderStyle, region, sheet);
        RegionUtil.setBorderBottom(borderStyle, region, sheet);
        RegionUtil.setBorderLeft(borderStyle, region, sheet);
        RegionUtil.setBorderRight(borderStyle, region, sheet);
    }
}
