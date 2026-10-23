package com.zlt.aps.lh.util;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.vo.LhDailyPlanProgressReportRowVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 日排产进度报表 Excel 生成工具。
 *
 * <p>版式沿用业务提供的模板：固定 21 列（胶种～T+2日排产信息）+ 动态日期列（T 日～报表末端）。
 * 动态日期列无法用占位符模板表达，因此使用 POI 直接生成：</p>
 * <ul>
 *   <li>第 1 行：「今日是N日」写在「T+2日排产信息」列；</li>
 *   <li>第 2 行：表头，主色 #008080，日产/硫化机数量/T+2日排产信息 及 T+2 日期列用 #00B0F0 强调；</li>
 *   <li>第 3 行起：数据。</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
public final class LhDailyPlanProgressExcelUtil {

    /** 固定列表头国际化 key（顺序与模板 A~U 列一致） */
    private static final String[] FIXED_HEADER_KEYS = {
            "ui.data.column.dailyPlanProgressReport.rubberType",
            "ui.data.column.dailyPlanProgressReport.productStatus",
            "ui.data.column.dailyPlanProgressReport.structureName",
            "ui.data.column.dailyPlanProgressReport.mainPattern",
            "ui.data.column.dailyPlanProgressReport.mouldCavityQty",
            "ui.data.column.dailyPlanProgressReport.typeBlockQty",
            "ui.data.column.dailyPlanProgressReport.mainMaterialDesc",
            "ui.data.column.dailyPlanProgressReport.materialCode",
            "ui.data.column.dailyPlanProgressReport.materialDesc",
            "ui.data.column.dailyPlanProgressReport.prodReqPlan",
            "ui.data.column.dailyPlanProgressReport.originalTotalQty",
            "ui.data.column.dailyPlanProgressReport.adjustQty1",
            "ui.data.column.dailyPlanProgressReport.adjustQty2",
            "ui.data.column.dailyPlanProgressReport.adjustQty3",
            "ui.data.column.dailyPlanProgressReport.structureType",
            "ui.data.column.dailyPlanProgressReport.finishQty",
            "ui.data.column.dailyPlanProgressReport.surplusQty",
            "ui.data.column.dailyPlanProgressReport.heightQty",
            "ui.data.column.dailyPlanProgressReport.dayLhQty",
            "ui.data.column.dailyPlanProgressReport.lhMachineQty",
            "ui.data.column.dailyPlanProgressReport.t2Info"
    };

    /** 固定列列宽（字符数，按业务模板） */
    private static final double[] FIXED_COLUMN_WIDTHS = {
            9.4D, 7.5D, 9.1D, 6.6D, 7.5D, 5.6D, 18.1D, 11.25D, 9.1D, 9.1D,
            9.1D, 8.0D, 8.0D, 8.0D, 9.0D, 9.0D, 9.0D, 9.0D, 7.0D, 9.0D, 17.3D
    };

    /** 日期列列宽（字符数） */
    private static final double DAY_COLUMN_WIDTH = 9.1D;

    /** 固定列数量（模板 A~U） */
    public static final int FIXED_COLUMN_COUNT = FIXED_HEADER_KEYS.length;

    /** 「T+2日排产信息」列下标 */
    private static final int T2_INFO_COLUMN_INDEX = 20;

    /** 「日产」「硫化机数量」「T+2日排产信息」三列的强调色起始下标 */
    private static final int HIGHLIGHT_COLUMN_START_INDEX = 18;

    /** 表头主色：深青 #008080 */
    private static final byte[] HEADER_MAIN_RGB = {(byte) 0x00, (byte) 0x80, (byte) 0x80};

    /** 表头强调色：浅蓝 #00B0F0 */
    private static final byte[] HEADER_HIGHLIGHT_RGB = {(byte) 0x00, (byte) 0xB0, (byte) 0xF0};

    /** T+2 日期列数据浅底 #DDF1FB */
    private static final byte[] T2_COLUMN_FILL_RGB = {(byte) 0xDD, (byte) 0xF1, (byte) 0xFB};

    /** 标题红字 #FF0000 */
    private static final byte[] TITLE_RED_RGB = {(byte) 0xFF, (byte) 0x00, (byte) 0x00};

    /** 「今日是{0}日」 */
    private static final String TODAY_TITLE_KEY = "ui.data.column.dailyPlanProgressReport.todayIs";

    /** 日期列头：同月「{0}号」 */
    private static final String DAY_LABEL_KEY = "ui.data.column.dailyPlanProgressReport.dayLabel";

    /** 日期列头：跨月「{0}月{1}日」 */
    private static final String DAY_LABEL_WITH_MONTH_KEY = "ui.data.column.dailyPlanProgressReport.dayLabelWithMonth";

    /** 分类：01-周期产品 */
    private static final String STRUCTURE_TYPE_CYCLE_KEY = "ui.data.column.dailyPlanProgressReport.structureTypeCycle";

    /** 分类：02-常规产品 */
    private static final String STRUCTURE_TYPE_NORMAL_KEY = "ui.data.column.dailyPlanProgressReport.structureTypeNormal";

    /** 结构类型：周期 */
    private static final String STRUCTURE_TYPE_CYCLE_CODE = "01";

    /** 结构类型：常规 */
    private static final String STRUCTURE_TYPE_NORMAL_CODE = "02";

    /** Sheet 名称 */
    private static final String SHEET_NAME_KEY = "ui.data.column.dailyPlanProgressReport.sheetName";

    private LhDailyPlanProgressExcelUtil() {
    }

    /**
     * 生成日排产进度报表 Excel。
     *
     * @param rowList       报表行
     * @param dayColumnList 日期列（T 日～报表末端，升序）
     * @param today         今日（T 日，用于首行「今日是N日」）
     * @return Excel 字节数组
     */
    public static byte[] build(List<LhDailyPlanProgressReportRowVO> rowList,
                               List<LocalDate> dayColumnList,
                               LocalDate today) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(I18nUtil.getMessage(SHEET_NAME_KEY));

            // 1. 样式准备
            CellStyle headerMainStyle = createHeaderStyle(workbook, HEADER_MAIN_RGB, false);
            CellStyle headerHighlightStyle = createHeaderStyle(workbook, HEADER_HIGHLIGHT_RGB, false);
            CellStyle headerT2InfoStyle = createHeaderStyle(workbook, HEADER_HIGHLIGHT_RGB, true);
            CellStyle textCenterStyle = createTextStyle(workbook, HorizontalAlignment.CENTER, null);
            CellStyle textLeftStyle = createTextStyle(workbook, HorizontalAlignment.LEFT, null);
            CellStyle numberStyle = createNumberStyle(workbook, null);
            CellStyle t2ColumnNumberStyle = createNumberStyle(workbook, T2_COLUMN_FILL_RGB);

            // 2. 第 1 行：今日是N日（写在 T+2日排产信息 列）
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(T2_INFO_COLUMN_INDEX);
            titleCell.setCellValue(MessageFormat.format(
                    I18nUtil.getMessage(TODAY_TITLE_KEY),
                    Objects.isNull(today) ? "" : String.valueOf(today.getDayOfMonth())));
            titleCell.setCellStyle(textCenterStyle);

            // 3. 第 2 行：表头
            int t2DayColumnIndex = resolveT2DayColumnIndex(dayColumnList, today);
            Row headerRow = sheet.createRow(1);
            for (int columnIndex = 0; columnIndex < FIXED_COLUMN_COUNT; columnIndex++) {
                Cell headerCell = headerRow.createCell(columnIndex);
                headerCell.setCellValue(I18nUtil.getMessage(FIXED_HEADER_KEYS[columnIndex]));
                if (columnIndex == T2_INFO_COLUMN_INDEX) {
                    headerCell.setCellStyle(headerT2InfoStyle);
                } else if (columnIndex >= HIGHLIGHT_COLUMN_START_INDEX) {
                    headerCell.setCellStyle(headerHighlightStyle);
                } else {
                    headerCell.setCellStyle(headerMainStyle);
                }
            }
            for (int dayIndex = 0; dayIndex < dayColumnList.size(); dayIndex++) {
                LocalDate day = dayColumnList.get(dayIndex);
                Cell headerCell = headerRow.createCell(FIXED_COLUMN_COUNT + dayIndex);
                headerCell.setCellValue(buildDayHeaderText(dayColumnList, dayIndex));
                headerCell.setCellStyle(dayIndex == t2DayColumnIndex
                        ? headerHighlightStyle : headerMainStyle);
            }

            // 4. 第 3 行起：数据
            int rowIndex = 2;
            for (LhDailyPlanProgressReportRowVO rowVO : rowList) {
                Row dataRow = sheet.createRow(rowIndex++);
                int columnIndex = 0;
                // 文本列
                setTextCell(dataRow, columnIndex++, rowVO.getRubberType(), textLeftStyle);
                setTextCell(dataRow, columnIndex++, rowVO.getProductStatus(), textCenterStyle);
                setTextCell(dataRow, columnIndex++, rowVO.getStructureName(), textCenterStyle);
                setTextCell(dataRow, columnIndex++, rowVO.getMainPattern(), textCenterStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getMouldCavityQty(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getTypeBlockQty(), numberStyle);
                setTextCell(dataRow, columnIndex++, rowVO.getMainMaterialDesc(), textLeftStyle);
                setTextCell(dataRow, columnIndex++, rowVO.getMaterialCode(), textLeftStyle);
                setTextCell(dataRow, columnIndex++, rowVO.getMaterialDesc(), textLeftStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getProdReqPlan(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getOriginalTotalQty(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getAdjustQty1(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getAdjustQty2(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getAdjustQty3(), numberStyle);
                setTextCell(dataRow, columnIndex++, resolveStructureTypeLabel(rowVO.getStructureType()),
                        textCenterStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getFinishQty(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getSurplusQty(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getHeightQty(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getDayLhQty(), numberStyle);
                setNumberCell(dataRow, columnIndex++, rowVO.getLhMachineQty(), numberStyle);
                setTextCell(dataRow, columnIndex, rowVO.getT2Info(), textCenterStyle);
                // 日期列
                List<Integer> dayQtyList = rowVO.getDayQtyList();
                for (int dayIndex = 0; dayIndex < dayColumnList.size(); dayIndex++) {
                    Integer dayQty = Objects.isNull(dayQtyList) || dayIndex >= dayQtyList.size()
                            ? null : dayQtyList.get(dayIndex);
                    setNumberCell(dataRow, FIXED_COLUMN_COUNT + dayIndex, dayQty,
                            dayIndex == t2DayColumnIndex ? t2ColumnNumberStyle : numberStyle);
                }
            }

            // 5. 列宽与冻结
            for (int columnIndex = 0; columnIndex < FIXED_COLUMN_COUNT; columnIndex++) {
                sheet.setColumnWidth(columnIndex, toColumnWidth(FIXED_COLUMN_WIDTHS[columnIndex]));
            }
            for (int dayIndex = 0; dayIndex < dayColumnList.size(); dayIndex++) {
                sheet.setColumnWidth(FIXED_COLUMN_COUNT + dayIndex, toColumnWidth(DAY_COLUMN_WIDTH));
            }
            // 冻结前 2 行与固定列，滚动日期列时表头与 SKU 信息始终可见
            sheet.createFreezePane(FIXED_COLUMN_COUNT, 2);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("生成日排产进度报表失败", e);
            throw new IllegalStateException("生成日排产进度报表失败", e);
        }
    }

    /**
     * 定位 T+2 日期列下标（日期列内为 today+1）。
     *
     * @param dayColumnList 日期列
     * @param today         今日（T 日）
     * @return T+2 列下标，未命中返回 -1
     */
    private static int resolveT2DayColumnIndex(List<LocalDate> dayColumnList, LocalDate today) {
        if (Objects.isNull(today)) {
            return -1;
        }
        LocalDate t2Date = today.plusDays(2);
        for (int dayIndex = 0; dayIndex < dayColumnList.size(); dayIndex++) {
            if (t2Date.equals(dayColumnList.get(dayIndex))) {
                return dayIndex;
            }
        }
        return -1;
    }

    /**
     * 构建日期列头文本：同月显示「N号」，跨月首列显示「M月N日」。
     *
     * @param dayColumnList 日期列
     * @param dayIndex      当前列下标
     * @return 列头文本
     */
    private static String buildDayHeaderText(List<LocalDate> dayColumnList, int dayIndex) {
        LocalDate day = dayColumnList.get(dayIndex);
        boolean crossMonthHeader = dayIndex == 0
                || dayColumnList.get(dayIndex - 1).getMonthValue() != day.getMonthValue();
        if (crossMonthHeader) {
            return MessageFormat.format(I18nUtil.getMessage(DAY_LABEL_WITH_MONTH_KEY),
                    String.valueOf(day.getMonthValue()), String.valueOf(day.getDayOfMonth()));
        }
        return MessageFormat.format(I18nUtil.getMessage(DAY_LABEL_KEY),
                String.valueOf(day.getDayOfMonth()));
    }

    /**
     * 解析结构类型（分类）展示文本。
     *
     * @param structureType 结构类型编码（01-周期、02-常规）
     * @return 展示文本
     */
    private static String resolveStructureTypeLabel(String structureType) {
        if (StringUtils.equals(STRUCTURE_TYPE_CYCLE_CODE, structureType)) {
            return I18nUtil.getMessage(STRUCTURE_TYPE_CYCLE_KEY);
        }
        if (StringUtils.equals(STRUCTURE_TYPE_NORMAL_CODE, structureType)) {
            return I18nUtil.getMessage(STRUCTURE_TYPE_NORMAL_KEY);
        }
        return StringUtils.defaultString(structureType);
    }

    /**
     * 创建表头样式。
     *
     * @param workbook  工作簿
     * @param fillRgb   背景色
     * @param redFont   是否红色字体
     * @return 样式
     */
    private static CellStyle createHeaderStyle(XSSFWorkbook workbook, byte[] fillRgb, boolean redFont) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setFillForegroundColor(new XSSFColor(fillRgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setThinBorder(style);
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        font.setColor(redFont ? new XSSFColor(TITLE_RED_RGB, null) : new XSSFColor(new byte[]{
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
        style.setFont(font);
        return style;
    }

    /**
     * 创建文本样式。
     *
     * @param workbook  工作簿
     * @param alignment 水平对齐方式
     * @param fillRgb   背景色（为空时不填充）
     * @return 样式
     */
    private static CellStyle createTextStyle(XSSFWorkbook workbook, HorizontalAlignment alignment,
                                             byte[] fillRgb) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(style);
        if (Objects.nonNull(fillRgb)) {
            style.setFillForegroundColor(new XSSFColor(fillRgb, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    /**
     * 创建数值样式（右对齐，千分位）。
     *
     * @param workbook 工作簿
     * @param fillRgb  背景色（为空时不填充）
     * @return 样式
     */
    private static CellStyle createNumberStyle(XSSFWorkbook workbook, byte[] fillRgb) {
        XSSFCellStyle style = (XSSFCellStyle) createTextStyle(workbook, HorizontalAlignment.RIGHT, fillRgb);
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("#,##0"));
        return style;
    }

    /**
     * 设置四边细边框。
     *
     * @param style 样式
     */
    private static void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * 写文本单元格。
     *
     * @param row       行
     * @param columnIndex 列下标
     * @param value     文本
     * @param style     样式
     */
    private static void setTextCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        if (StringUtils.isNotEmpty(value)) {
            cell.setCellValue(value);
        }
    }

    /**
     * 写数值单元格。
     *
     * @param row       行
     * @param columnIndex 列下标
     * @param value     数值
     * @param style     样式
     */
    private static void setNumberCell(Row row, int columnIndex, Integer value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        if (Objects.nonNull(value)) {
            cell.setCellValue(value.doubleValue());
        }
    }

    /**
     * 字符宽度转 POI 列宽单位（1/256 字符）。
     *
     * @param widthInChars 字符宽度
     * @return POI 列宽
     */
    private static int toColumnWidth(double widthInChars) {
        return (int) Math.round(widthInChars * 256);
    }
}
