package com.zlt.aps.common.core.utils;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.domain.dto.ScheduleAnalysisReportQueryDTO;
import com.zlt.aps.common.core.domain.vo.ScheduleAnalysisReportRowVO;
import com.zlt.aps.common.core.domain.vo.ScheduleAnalysisReportVO;
import com.zlt.aps.common.core.domain.vo.ScheduleAnalysisShiftHeaderVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划分析报表公共Excel模板写出组件。
 *
 * <p>各模块页面列顺序不同，导出模板按 {@link ScheduleAnalysisReportColumnLayout} 选择，
 * 保证导出Excel的列顺序与前端页面一致。</p>
 */
public final class ScheduleAnalysisReportExcelWriter {

    private static final DateTimeFormatter QUERY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT);
    private static final DateTimeFormatter SHIFT_TITLE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd", Locale.ROOT);
    private static final String SHIFT_TITLE_FORMAT_KEY = "ui.data.column.cxScheduleResult.shift.titleFormat";
    private static final String SHIFT_NAME_KEY_PREFIX = "ui.data.column.cxScheduleResult.shift.";
    /**
     * 库存行与计划行整行底色（ARGB：#FFDAEEF3），与前端页面的库存/计划行区分保持一致
     */
    private static final byte[] STOCK_PLAN_ROW_FILL_ARGB = {(byte) 0xFF, (byte) 0xDA, (byte) 0xEE, (byte) 0xF3};
    private static final String[] CLASS_KEYS = {
            "classOne", "classTwo", "classThree", "classFour",
            "classFive", "classSix", "classSeven", "classEight"
    };

    private ScheduleAnalysisReportExcelWriter() {
    }

    /**
     * 使用标准布局（工厂、排程日期在前）写出一次完整的报表查询结果。
     *
     * @param report           本次报表查询结果
     * @param query            本次查询条件
     * @param reportTitle      报表标题
     * @param stockTypeLabel   库存类别显示文本
     * @param planTypeLabel    计划类别显示文本
     * @param productCodeLabel 当前模块物料编码标题
     * @param productNameLabel 当前模块物料名称标题
     * @param factoryName      工厂字典名称，缺少时回退到工厂编码
     * @return XLSX文件字节
     * @throws IOException 模板不存在或工作簿写出失败时抛出
     */
    public static byte[] write(ScheduleAnalysisReportVO report, ScheduleAnalysisReportQueryDTO query,
                               String reportTitle, String stockTypeLabel, String planTypeLabel,
                               String productCodeLabel, String productNameLabel, String factoryName)
            throws IOException {
        return write(report, query, reportTitle, stockTypeLabel, planTypeLabel, productCodeLabel,
                productNameLabel, factoryName, ScheduleAnalysisReportColumnLayout.STANDARD, null);
    }

    /**
     * 按指定列布局写出一次完整的报表查询结果。
     *
     * @param report           本次报表查询结果
     * @param query            本次查询条件
     * @param reportTitle      报表标题
     * @param stockTypeLabel   库存类别显示文本
     * @param planTypeLabel    计划类别显示文本
     * @param productCodeLabel 当前模块物料编码标题
     * @param productNameLabel 当前模块物料名称标题
     * @param factoryName      工厂字典名称，缺少时回退到工厂编码
     * @param layout           导出Excel的列布局，需与前端页面列顺序一致
     * @return XLSX文件字节
     * @throws IOException 模板不存在或工作簿写出失败时抛出
     */
    public static byte[] write(ScheduleAnalysisReportVO report, ScheduleAnalysisReportQueryDTO query,
                               String reportTitle, String stockTypeLabel, String planTypeLabel,
                               String productCodeLabel, String productNameLabel, String factoryName,
                               ScheduleAnalysisReportColumnLayout layout)
            throws IOException {
        return write(report, query, reportTitle, stockTypeLabel, planTypeLabel, productCodeLabel,
                productNameLabel, factoryName, layout, null);
    }

    /**
     * 按指定列布局和类别标题写出一次完整的报表查询结果。
     *
     * @param report           本次报表查询结果
     * @param query            本次查询条件
     * @param reportTitle      报表标题
     * @param stockTypeLabel   库存类别显示文本
     * @param planTypeLabel    计划类别显示文本
     * @param productCodeLabel 当前模块物料编码标题
     * @param productNameLabel 当前模块物料名称标题
     * @param factoryName      工厂字典名称，缺少时回退到工厂编码
     * @param layout           导出Excel的列布局，需与前端页面列顺序一致
     * @param categoryLabel    可选类别列标题，为空时沿用公共标题
     * @return XLSX文件字节
     * @throws IOException 模板不存在或工作簿写出失败时抛出
     */
    public static byte[] write(ScheduleAnalysisReportVO report, ScheduleAnalysisReportQueryDTO query,
                               String reportTitle, String stockTypeLabel, String planTypeLabel,
                               String productCodeLabel, String productNameLabel, String factoryName,
                               ScheduleAnalysisReportColumnLayout layout, String categoryLabel)
            throws IOException {
        ScheduleAnalysisReportColumnLayout resolvedLayout = layout == null
                ? ScheduleAnalysisReportColumnLayout.STANDARD : layout;
        ScheduleAnalysisReportExcelWriter writer = new ScheduleAnalysisReportExcelWriter();
        Map<String, Object> context = writer.buildContext(report, query, reportTitle, productCodeLabel,
                productNameLabel, factoryName, categoryLabel);
        List<Map<String, Object>> rowData = writer.buildRowData(report, stockTypeLabel, planTypeLabel, factoryName,
                query.getFactoryCode());
        List<List<Map<String, Object>>> dataLists = Collections.singletonList(rowData);
        try (InputStream templateInputStream = ScheduleAnalysisReportExcelWriter.class.getClassLoader()
                .getResourceAsStream(resolvedLayout.getTemplateResource())) {
            if (templateInputStream == null) {
                throw new IOException(I18nUtil.getMessage("ui.scheduleAnalysisReport.templateMissing"));
            }
            try {
                byte[] templateBytes = ExcelUtils.writeMultiList(templateInputStream, 0, context, dataLists);
                return writer.formatAndRenameWorksheet(templateBytes, report, reportTitle, resolvedLayout);
            } catch (ExcelUtils.ExcelException | IOException exception) {
                throw new IOException(I18nUtil.getMessage("ui.scheduleAnalysisReport.exportFailed"), exception);
            }
        }
    }

    /**
     * 组装模板中的标题、筛选信息、查询时间和动态表头占位符。
     *
     * @param report           本次报表查询结果
     * @param query            本次查询条件
     * @param reportTitle      报表标题
     * @param productCodeLabel 当前模块物料编码标题
     * @param productNameLabel 当前模块物料名称标题
     * @param factoryName      工厂字典名称，缺少时回退到工厂编码
     * @return 模板上下文数据
     */
    private Map<String, Object> buildContext(ScheduleAnalysisReportVO report,
                                             ScheduleAnalysisReportQueryDTO query,
                                             String reportTitle, String productCodeLabel,
                                             String productNameLabel, String factoryName,
                                             String categoryLabel) {
        Map<String, Object> context = new HashMap<>();
        context.put("title", reportTitle);
        context.put("factoryLabel", I18nUtil.getMessage("ui.scheduleAnalysisReport.factory"));
        context.put("factoryName", factoryName == null ? query.getFactoryCode() : factoryName);
        context.put("scheduleDateLabel", I18nUtil.getMessage("ui.scheduleAnalysisReport.scheduleDate"));
        context.put("scheduleDate", query.getScheduleDate().toString());
        context.put("filterLabel", I18nUtil.getMessage("ui.scheduleAnalysisReport.filter"));
        context.put("filterSummary", this.buildFilterSummary(query, productCodeLabel, productNameLabel));
        context.put("queriedAtLabel", I18nUtil.getMessage("ui.scheduleAnalysisReport.queriedAt"));
        context.put("queriedAt", this.formatQueriedAt(report.getQueriedAt(), report.getQueriedAtZoneId()));
        context.put("factoryHeader", I18nUtil.getMessage("ui.scheduleAnalysisReport.factory"));
        context.put("scheduleDateHeader", I18nUtil.getMessage("ui.scheduleAnalysisReport.scheduleDate"));
        context.put("categoryHeader", StringUtils.isNotBlank(categoryLabel) ? categoryLabel
                : I18nUtil.getMessage("ui.scheduleAnalysisReport.embryoOrCategory"));
        context.put("productCodeHeader", productCodeLabel);
        context.put("productNameHeader", productNameLabel);
        context.put("sixClockStockHeader", report.getStockBaselineLabel() == null
                ? I18nUtil.getMessage("ui.scheduleAnalysisReport.sixClockStock") : report.getStockBaselineLabel());
        context.put("machineHeader", I18nUtil.getMessage("ui.scheduleAnalysisReport.machine"));
        this.putShiftHeaderValues(context, report.getShiftHeaders());
        return context;
    }

    /**
     * 生成包含筛选字段名称和值的简要说明。
     *
     * @param query            查询条件
     * @param productCodeLabel 当前模块物料编码标题
     * @param productNameLabel 当前模块物料名称标题
     * @return 筛选条件摘要
     */
    private String buildFilterSummary(ScheduleAnalysisReportQueryDTO query,
                                      String productCodeLabel, String productNameLabel) {
        return productCodeLabel + "=" + this.emptyText(query.getProductCode())
                + "; " + productNameLabel + "=" + this.emptyText(query.getProductName())
                + "; " + I18nUtil.getMessage("ui.scheduleAnalysisReport.embryoCode") + "="
                + this.emptyText(query.getEmbryoCode());
    }

    /**
     * 将八个班次的动态标题写入模板上下文。
     *
     * @param context      模板上下文
     * @param shiftHeaders 班次配置表头
     * @throws DateTimeParseException 班次日期不是ISO日期格式时抛出
     */
    private void putShiftHeaderValues(Map<String, Object> context,
                                      List<ScheduleAnalysisShiftHeaderVO> shiftHeaders) {
        for (int index = 0; index < CLASS_KEYS.length; index++) {
            ScheduleAnalysisShiftHeaderVO shiftHeader = shiftHeaders != null && shiftHeaders.size() > index
                    ? shiftHeaders.get(index) : null;
            String classKey = CLASS_KEYS[index];
            String shiftType = shiftHeader == null ? null : shiftHeader.getShiftType();
            String shiftDate = shiftHeader == null ? "" : this.formatShiftTitleDate(shiftHeader.getShiftDate());
            String chineseTitle = this.formatShiftTitle(shiftType, shiftDate, Locale.CHINA);
            String vietnameseTitle = this.formatShiftTitle(shiftType, shiftDate, new Locale("vi", "VN"));
            context.put(classKey + "Header", chineseTitle + "\n" + vietnameseTitle);
        }
    }

    /**
     * 按班次类型和指定语言生成班次名称与月日标题。
     *
     * @param shiftType 班次类型
     * @param shiftDate 月日文本
     * @param locale    标题语言
     * @return 班次名称及月日，日期缺失时只返回班次名称
     */
    private String formatShiftTitle(String shiftType, String shiftDate, Locale locale) {
        if (shiftType == null || shiftType.trim().isEmpty()) {
            return "";
        }
        String shiftName = I18nUtil.getMessage(SHIFT_NAME_KEY_PREFIX + shiftType, locale);
        if (shiftDate == null || shiftDate.isEmpty()) {
            return shiftName;
        }
        String titleFormat = I18nUtil.getMessage(SHIFT_TITLE_FORMAT_KEY, locale);
        return MessageFormat.format(titleFormat, shiftName, shiftDate);
    }

    /**
     * 将班次日期格式化为月/日，不包含年份。
     *
     * @param shiftDate ISO格式班次日期
     * @return MM/dd格式日期，空日期返回空文本
     * @throws DateTimeParseException 班次日期不是ISO日期格式时抛出
     */
    private String formatShiftTitleDate(String shiftDate) {
        if (shiftDate == null || shiftDate.trim().isEmpty()) {
            return "";
        }
        return LocalDate.parse(shiftDate).format(SHIFT_TITLE_DATE_FORMATTER);
    }

    /**
     * 组装模板重复数据行，并仅为负库存单元格附加红色字体样式。
     *
     * @param report         本次报表查询结果
     * @param stockTypeLabel 库存类别显示文本
     * @param planTypeLabel  计划类别显示文本
     * @param factoryName    工厂字典名称
     * @param factoryCode    工厂编码
     * @return Excel模板数据行
     */
    private List<Map<String, Object>> buildRowData(ScheduleAnalysisReportVO report,
                                                   String stockTypeLabel, String planTypeLabel,
                                                   String factoryName, String factoryCode) {
        if (report.getRows() == null) {
            return new ArrayList<>();
        }
        Map<String, Object> negativeNumberStyle = new HashMap<>();
        negativeNumberStyle.put("border", false);
        negativeNumberStyle.put("fontColor", IndexedColors.RED.getIndex());
        return report.getRows().stream()
                .map(item -> this.buildRowData(item, stockTypeLabel, planTypeLabel, factoryName,
                        factoryCode, negativeNumberStyle))
                .collect(Collectors.toList());
    }

    /**
     * 将报表明细映射为模板重复行中的字段占位符数据。
     *
     * @param item                报表明细
     * @param stockTypeLabel      库存类别显示文本
     * @param planTypeLabel       计划类别显示文本
     * @param factoryName         工厂字典名称
     * @param factoryCode         工厂编码
     * @param negativeNumberStyle 负数单元格字体样式
     * @return Excel模板行数据
     */
    private Map<String, Object> buildRowData(ScheduleAnalysisReportRowVO item,
                                             String stockTypeLabel, String planTypeLabel,
                                             String factoryName, String factoryCode,
                                             Map<String, Object> negativeNumberStyle) {
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("factoryName", factoryName == null ? factoryCode : factoryName);
        rowData.put("scheduleDate", item.getScheduleDate());
        rowData.put("category", this.resolveCategory(item, stockTypeLabel, planTypeLabel));
        rowData.put("productCode", item.getProductCode());
        rowData.put("productName", item.getProductName());
        rowData.put("sixClockStockQty", item.getSixClockStockQty());
        List<BigDecimal> classValues = Arrays.asList(item.getClass1(), item.getClass2(), item.getClass3(),
                item.getClass4(), item.getClass5(), item.getClass6(), item.getClass7(), item.getClass8());
        for (int index = 0; index < CLASS_KEYS.length; index++) {
            String classKey = CLASS_KEYS[index];
            BigDecimal classValue = classValues.get(index);
            rowData.put(classKey, classValue);
            if (Objects.equals(item.getDataType(), 2) && classValue != null
                    && classValue.compareTo(BigDecimal.ZERO) < 0) {
                rowData.put("style_" + classKey, negativeNumberStyle);
            }
        }
        if (Objects.equals(item.getDataType(), 1)) {
            rowData.put("machineValue", item.getMachineCodes());
        } else if (Objects.equals(item.getDataType(), 3)) {
            rowData.put("machineValue", item.getMachineCount());
        }
        return rowData;
    }

    /**
     * 根据明细类型确定类别列显示内容。
     *
     * @param item           报表明细
     * @param stockTypeLabel 库存类别显示文本
     * @param planTypeLabel  计划类别显示文本
     * @return 胎胚描述（缺失时回退胎胚编码）、库存类别或计划类别
     */
    private String resolveCategory(ScheduleAnalysisReportRowVO item,
                                   String stockTypeLabel, String planTypeLabel) {
        if (Objects.equals(item.getDataType(), 1)) {
            // 胎胚列优先展示胎胚描述，描述为空时回退胎胚编码
            return StringUtils.isNotBlank(item.getEmbryoDesc()) ? item.getEmbryoDesc() : item.getEmbryoCode();
        }
        if (Objects.equals(item.getDataType(), 2)) {
            return stockTypeLabel;
        }
        if (Objects.equals(item.getDataType(), 3)) {
            return planTypeLabel;
        }
        return "";
    }

    /**
     * 按报表关联的工厂时区格式化查询完成时间。
     *
     * @param queriedAt 查询完成时间
     * @param zoneId    工厂时区标识
     * @return 带时区偏移的查询完成时间文本
     * @throws DateTimeException 工厂时区标识无效时抛出
     */
    private String formatQueriedAt(Date queriedAt, String zoneId) {
        if (queriedAt == null) {
            return "";
        }
        ZoneId queryZone = zoneId == null || zoneId.trim().isEmpty() ? ZoneOffset.UTC : ZoneId.of(zoneId);
        return QUERY_TIME_FORMATTER.format(queriedAt.toInstant().atZone(queryZone));
    }

    /**
     * 修正整数数值显示格式、按布局补充库存/计划行底色，并按报表标题重命名工作表。
     *
     * @param workbookBytes 模板填充后的Excel字节
     * @param report        本次报表查询结果，用于定位库存行与计划行
     * @param reportTitle   报表标题
     * @param layout        导出Excel的列布局，决定数值列范围及是否需要行底色
     * @return 工作表名称更新后的Excel字节
     * @throws IOException 工作簿读取或写出失败时抛出
     */
    private byte[] formatAndRenameWorksheet(byte[] workbookBytes, ScheduleAnalysisReportVO report,
                                            String reportTitle,
                                            ScheduleAnalysisReportColumnLayout layout) throws IOException {
        try (InputStream workbookInputStream = new ByteArrayInputStream(workbookBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(workbookInputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            this.formatIntegralNumericCells(workbook, layout);
            if (layout == ScheduleAnalysisReportColumnLayout.EMBRYO_FIRST) {
                // 垫胶/内衬导出与页面保持一致，库存行与计划行整行浅蓝底色；裁断等布局维持原样
                this.applyStockPlanRowFill(workbook, report);
            }
            if (reportTitle != null && !reportTitle.trim().isEmpty()) {
                workbook.setSheetName(0, WorkbookUtil.createSafeSheetName(reportTitle));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 为库存行（dataType=2）与计划行（dataType=3）整行填充浅蓝底色。
     *
     * <p>模板数据行按报表行顺序从第2行开始逐行写入，因此报表行下标加1即为工作表行下标。
     * 负库存单元格的红色字体样式在填充底色前已单独设置，此处基于其现有样式克隆，
     * 保证红字与底色同时保留。</p>
     *
     * @param workbook 已填充报表数据的工作簿
     * @param report   本次报表查询结果
     */
    private void applyStockPlanRowFill(XSSFWorkbook workbook, ScheduleAnalysisReportVO report) {
        if (report == null || report.getRows() == null || report.getRows().isEmpty()) {
            return;
        }
        Sheet sheet = workbook.getSheetAt(0);
        XSSFColor fillColor = new XSSFColor(STOCK_PLAN_ROW_FILL_ARGB, null);
        Map<Short, XSSFCellStyle> filledStyles = new HashMap<>();
        for (int rowIndex = 0; rowIndex < report.getRows().size(); rowIndex++) {
            ScheduleAnalysisReportRowVO item = report.getRows().get(rowIndex);
            if (!Objects.equals(item.getDataType(), 2) && !Objects.equals(item.getDataType(), 3)) {
                continue;
            }
            Row row = sheet.getRow(rowIndex + 1);
            if (row == null) {
                continue;
            }
            for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null || !(cell.getCellStyle() instanceof XSSFCellStyle)) {
                    continue;
                }
                XSSFCellStyle originalStyle = (XSSFCellStyle) cell.getCellStyle();
                XSSFCellStyle filledStyle = filledStyles.computeIfAbsent(originalStyle.getIndex(), styleIndex -> {
                    XSSFCellStyle copiedStyle = workbook.createCellStyle();
                    copiedStyle.cloneStyleFrom(originalStyle);
                    copiedStyle.setFillForegroundColor(fillColor);
                    copiedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    return copiedStyle;
                });
                cell.setCellStyle(filledStyle);
            }
        }
    }

    /**
     * 将报表数据区内的整数数值设为整数显示格式，同时保留原有单元格样式。
     *
     * @param workbook 已填充报表数据的工作簿
     * @param layout   导出Excel的列布局，决定需要处理的数值列范围
     */
    private void formatIntegralNumericCells(XSSFWorkbook workbook, ScheduleAnalysisReportColumnLayout layout) {
        Sheet sheet = workbook.getSheetAt(0);
        DataFormat dataFormat = workbook.createDataFormat();
        Map<Short, CellStyle> integerStyles = new HashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row currentRow = sheet.getRow(rowIndex);
            if (currentRow == null) {
                continue;
            }
            for (int columnIndex = layout.getFirstNumericColumnIndex();
                 columnIndex <= layout.getLastNumericColumnIndex(); columnIndex++) {
                Cell currentCell = currentRow.getCell(columnIndex);
                if (currentCell == null || currentCell.getCellType() != CellType.NUMERIC) {
                    continue;
                }
                double numericValue = currentCell.getNumericCellValue();
                if (numericValue != Math.rint(numericValue)) {
                    continue;
                }
                CellStyle originalStyle = currentCell.getCellStyle();
                short originalStyleIndex = originalStyle.getIndex();
                CellStyle integerStyle = integerStyles.computeIfAbsent(originalStyleIndex, styleIndex -> {
                    CellStyle copiedStyle = workbook.createCellStyle();
                    copiedStyle.cloneStyleFrom(originalStyle);
                    copiedStyle.setDataFormat(dataFormat.getFormat("0"));
                    return copiedStyle;
                });
                currentCell.setCellStyle(integerStyle);
            }
        }
    }

    /**
     * 将空筛选值转为空白文本。
     *
     * @param value 筛选值
     * @return 空值对应空文本
     */
    private String emptyText(String value) {
        return value == null ? "" : value;
    }
}
