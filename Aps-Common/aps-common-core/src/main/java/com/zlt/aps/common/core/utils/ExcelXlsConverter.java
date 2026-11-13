package com.zlt.aps.common.core.utils;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPrintSetup;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * xlsx（OOXML / XSSF）转 xls（BIFF8 / HSSF）转换器。
 *
 * <p>使用场景：导出模板是 .xlsx，但下游要求 .xls 文件（如只认 BIFF8 的导入程序）。POI 没有提供
 * XSSFWorkbook → HSSFWorkbook 的直接转换，因此按「逐 sheet 重建」的方式实现：读取源工作簿的单元格值、
 * 公式、样式、行列布局，在 HSSFWorkbook 中重建一遍。</p>
 *
 * <p><b>会保留</b>：单元格值（文本/数值/日期/布尔/公式/错误）、字体（名称/字号/粗体/斜体/下划线/删除线/
 * 上下标/颜色）、数字格式（按格式串重新注册，不依赖样式索引）、对齐、自动换行、缩进、旋转、边框及边框颜色、
 * 填充图案及前景/背景色、行高、列宽、隐藏行列、合并单元格、冻结/拆分窗格、打印设置（纸张/缩放/横纵向/页边距）。</p>
 *
 * <p><b>不保留</b>：条件格式、数据校验（下拉/校验提示）、图片与图表、批注、超链接、定义名称、数据透视表。
 * 这些特性在 xls 中的承载模型与 xlsx 差异较大，跨工作簿复制需要逐项重建，本类不做处理。若源文件依赖
 * 条件格式做业务提示，请改用 xlsx 输出。</p>
 *
 * <p><b>限制</b>：xls 单 sheet 上限 65536 行 × 256 列，超出时直接抛出异常（不做截断，避免静默丢数据）。</p>
 */
public final class ExcelXlsConverter {

    /** xls（BIFF8）单 sheet 最大行数 */
    private static final int XLS_MAX_ROWS = 65536;

    /** xls（BIFF8）单 sheet 最大列数 */
    private static final int XLS_MAX_COLUMNS = 256;

    /** xls 的 sheet 名最大长度 */
    private static final int XLS_MAX_SHEET_NAME_LENGTH = 31;

    /** 自动色（跟随主题/默认），xls 中以索引 64 表示 */
    private static final short AUTOMATIC_COLOR_INDEX = IndexedColors.AUTOMATIC.getIndex();

    private ExcelXlsConverter() {
    }

    /**
     * 把 xlsx 字节流转换为 xls 字节流。
     *
     * @param xlsxBytes xlsx 文件内容
     * @return xls 文件内容
     * @throws IllegalArgumentException 入参为空
     * @throws IllegalStateException    转换过程失败（含超出行列上限）
     */
    public static byte[] convert(byte[] xlsxBytes) {
        if (xlsxBytes == null || xlsxBytes.length == 0) {
            throw new IllegalArgumentException("xlsx 内容为空，无法转换为 xls");
        }
        try (XSSFWorkbook source = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes));
             HSSFWorkbook target = new HSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            StyleCopier styleCopier = new StyleCopier(source, target);
            // 公式求值器：xls 不持久化 fullCalcOnLoad 标记，只写公式的话，不自动重算的查看器会显示 0，
            // 因此复制公式时同步写入求值结果作为缓存值
            FormulaEvaluator evaluator = source.getCreationHelper().createFormulaEvaluator();
            for (int sheetIndex = 0; sheetIndex < source.getNumberOfSheets(); sheetIndex++) {
                copySheet(source.getSheetAt(sheetIndex), target, styleCopier, evaluator);
            }
            // 兜底：公式求值失败时（函数不支持等）没有缓存值，仍希望 Excel 打开时重算一次
            target.setForceFormulaRecalculation(true);
            target.write(outputStream);
            return outputStream.toByteArray();
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("xlsx 转换为 xls 失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 复制单个 sheet：布局 → 行与单元格 → 合并区域 → 窗格 → 打印设置。
     */
    private static void copySheet(XSSFSheet source, HSSFWorkbook target, StyleCopier styleCopier,
            FormulaEvaluator evaluator) {
        HSSFSheet sheet = target.createSheet(resolveSheetName(source));
        sheet.setDefaultColumnWidth(source.getDefaultColumnWidth());
        sheet.setDefaultRowHeightInPoints(source.getDefaultRowHeightInPoints());
        sheet.setDisplayGridlines(source.isDisplayGridlines());

        int lastRowNum = source.getLastRowNum();
        if (lastRowNum + 1 > XLS_MAX_ROWS) {
            throw new IllegalStateException("sheet[" + source.getSheetName() + "] 共 " + (lastRowNum + 1)
                    + " 行，超出 xls 单页 " + XLS_MAX_ROWS + " 行上限，请改用 xlsx 导出");
        }

        // 先按行扫描出实际使用的最大列，再统一设置列宽与隐藏状态
        int maxColumn = 0;
        for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
            XSSFRow row = source.getRow(rowIndex);
            if (row != null && row.getLastCellNum() > maxColumn) {
                maxColumn = row.getLastCellNum();
            }
        }
        if (maxColumn > XLS_MAX_COLUMNS) {
            throw new IllegalStateException("sheet[" + source.getSheetName() + "] 共 " + maxColumn
                    + " 列，超出 xls 单页 " + XLS_MAX_COLUMNS + " 列上限，请改用 xlsx 导出");
        }
        int defaultWidth = source.getDefaultColumnWidth() * 256;
        for (int columnIndex = 0; columnIndex < maxColumn; columnIndex++) {
            int width = source.getColumnWidth(columnIndex);
            if (width != defaultWidth) {
                sheet.setColumnWidth(columnIndex, width);
            }
            if (source.isColumnHidden(columnIndex)) {
                sheet.setColumnHidden(columnIndex, true);
            }
        }

        for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
            XSSFRow sourceRow = source.getRow(rowIndex);
            if (sourceRow == null) {
                continue;
            }
            HSSFRow targetRow = sheet.createRow(rowIndex);
            targetRow.setHeightInPoints(sourceRow.getHeightInPoints());
            if (sourceRow.getZeroHeight()) {
                targetRow.setZeroHeight(true);
            }
            for (int columnIndex = 0; columnIndex < sourceRow.getLastCellNum(); columnIndex++) {
                XSSFCell sourceCell = sourceRow.getCell(columnIndex);
                if (sourceCell == null) {
                    continue;
                }
                HSSFCell targetCell = targetRow.createCell(columnIndex);
                targetCell.setCellStyle(styleCopier.copy(sourceCell.getCellStyle()));
                copyCellValue(sourceCell, targetCell, evaluator);
            }
        }

        for (int index = 0; index < source.getNumMergedRegions(); index++) {
            CellRangeAddress region = source.getMergedRegion(index);
            sheet.addMergedRegion(region);
        }

        copyPaneInformation(source, sheet);
        copyPrintSetup(source, sheet);
    }

    /**
     * 复制单元格值。日期按数值+日期格式写入，保证 xls 中仍显示为日期而不是数字序列。
     */
    private static void copyCellValue(XSSFCell source, HSSFCell target, FormulaEvaluator evaluator) {
        CellType cellType = source.getCellType();
        if (cellType == null) {
            return;
        }
        switch (cellType) {
            case STRING:
                target.setCellValue(source.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(source)) {
                    target.setCellValue(source.getDateCellValue());
                } else {
                    target.setCellValue(source.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                target.setCellValue(source.getBooleanCellValue());
                break;
            case FORMULA:
                target.setCellFormula(source.getCellFormula());
                copyFormulaCachedValue(source, target, evaluator);
                break;
            case ERROR:
                target.setCellErrorValue(source.getErrorCellValue());
                break;
            default:
                break;
        }
    }

    /**
     * 公式单元格补写缓存值：xls 不会持久化「打开时重算」标记，只写公式的话，
     * 不支持自动重算的查看器（部分 WPS / 在线预览）会显示 0 或空白。
     *
     * <p>HSSF 中「先 setCellFormula 再 setCellValue」是把值写进公式的缓存结果，不会破坏公式本身。
     * 求值失败（不支持的函数、外部链接等）时只保留公式，交由 Excel 打开时重算。</p>
     */
    private static void copyFormulaCachedValue(XSSFCell source, HSSFCell target, FormulaEvaluator evaluator) {
        if (evaluator == null) {
            return;
        }
        try {
            CellValue cellValue = evaluator.evaluate(source);
            if (cellValue == null) {
                return;
            }
            switch (cellValue.getCellType()) {
                case NUMERIC:
                    target.setCellValue(cellValue.getNumberValue());
                    break;
                case STRING:
                    target.setCellValue(cellValue.getStringValue());
                    break;
                case BOOLEAN:
                    target.setCellValue(cellValue.getBooleanValue());
                    break;
                default:
                    break;
            }
        } catch (RuntimeException ignored) {
            // 公式无法求值时保留公式本身，不做中断
        }
    }

    /**
     * 复制冻结/拆分窗格。
     */
    private static void copyPaneInformation(XSSFSheet source, HSSFSheet target) {
        PaneInformation paneInformation = source.getPaneInformation();
        if (paneInformation == null) {
            return;
        }
        if (paneInformation.isFreezePane()) {
            target.createFreezePane(paneInformation.getVerticalSplitPosition(),
                    paneInformation.getHorizontalSplitPosition(),
                    paneInformation.getVerticalSplitLeftColumn(),
                    paneInformation.getHorizontalSplitTopRow());
        } else {
            target.createSplitPane(paneInformation.getVerticalSplitPosition(),
                    paneInformation.getHorizontalSplitPosition(),
                    paneInformation.getVerticalSplitLeftColumn(),
                    paneInformation.getHorizontalSplitTopRow(),
                    paneInformation.getActivePane());
        }
    }

    /**
     * 复制打印设置（纸张、缩放、横纵向、页边距），保证 xls 的打印效果与模板一致。
     */
    private static void copyPrintSetup(XSSFSheet source, HSSFSheet target) {
        PrintSetup sourceSetup = source.getPrintSetup();
        HSSFPrintSetup targetSetup = target.getPrintSetup();
        targetSetup.setPaperSize(sourceSetup.getPaperSize());
        targetSetup.setScale(sourceSetup.getScale());
        targetSetup.setFitWidth(sourceSetup.getFitWidth());
        targetSetup.setFitHeight(sourceSetup.getFitHeight());
        targetSetup.setLandscape(sourceSetup.getLandscape());
        targetSetup.setLeftToRight(sourceSetup.getLeftToRight());
        target.setFitToPage(source.getFitToPage());
        target.setMargin(Sheet.LeftMargin, source.getMargin(Sheet.LeftMargin));
        target.setMargin(Sheet.RightMargin, source.getMargin(Sheet.RightMargin));
        target.setMargin(Sheet.TopMargin, source.getMargin(Sheet.TopMargin));
        target.setMargin(Sheet.BottomMargin, source.getMargin(Sheet.BottomMargin));
        target.setMargin(Sheet.HeaderMargin, source.getMargin(Sheet.HeaderMargin));
        target.setMargin(Sheet.FooterMargin, source.getMargin(Sheet.FooterMargin));
    }

    /**
     * sheet 名截断到 xls 允许的 31 个字符。
     */
    private static String resolveSheetName(XSSFSheet source) {
        String sheetName = source.getSheetName();
        if (sheetName == null || sheetName.isEmpty()) {
            return "Sheet";
        }
        return sheetName.length() > XLS_MAX_SHEET_NAME_LENGTH
                ? sheetName.substring(0, XLS_MAX_SHEET_NAME_LENGTH) : sheetName;
    }

    /**
     * 样式转换器：把 XSSFCellStyle 转成等价的 HSSFCellStyle，并按源样式索引缓存，避免重复创建
     * （xls 单工作簿最多 4000 个单元格样式、1024 个字体）。
     *
     * <p>关键点：数字格式必须按「格式串」在目标工作簿重新注册。{@code CellStyle#cloneStyleFrom} 是跨工作簿
     * 按索引拷贝的，xlsx 的自定义格式索引（如 176/177）在 xls 中不存在，直接克隆会让格式错位——
     * 典型表现是 {@code 0;-0;;@} 这类格式失效、0 值重新显示出来。</p>
     */
    private static final class StyleCopier {

        private final HSSFWorkbook targetWorkbook;

        private final ThemesTable themesTable;

        private final HSSFPalette palette;

        /** 源样式索引 → 目标样式 */
        private final Map<Integer, HSSFCellStyle> styleCache = new HashMap<>();

        /** 字体特征 → 目标字体 */
        private final Map<String, HSSFFont> fontCache = new HashMap<>();

        private StyleCopier(XSSFWorkbook sourceWorkbook, HSSFWorkbook targetWorkbook) {
            this.targetWorkbook = targetWorkbook;
            this.themesTable = sourceWorkbook.getTheme();
            this.palette = targetWorkbook.getCustomPalette();
        }

        private HSSFCellStyle copy(CellStyle sourceStyle) {
            if (!(sourceStyle instanceof XSSFCellStyle)) {
                return null;
            }
            int styleIndex = sourceStyle.getIndex();
            HSSFCellStyle cachedStyle = styleCache.get(styleIndex);
            if (cachedStyle != null) {
                return cachedStyle;
            }
            XSSFCellStyle xssfStyle = (XSSFCellStyle) sourceStyle;
            HSSFCellStyle targetStyle = targetWorkbook.createCellStyle();
            styleCache.put(styleIndex, targetStyle);

            String dataFormat = xssfStyle.getDataFormatString();
            if (dataFormat != null && !dataFormat.isEmpty()) {
                targetStyle.setDataFormat(targetWorkbook.createDataFormat().getFormat(dataFormat));
            }
            targetStyle.setAlignment(xssfStyle.getAlignment());
            targetStyle.setVerticalAlignment(xssfStyle.getVerticalAlignment());
            targetStyle.setWrapText(xssfStyle.getWrapText());
            targetStyle.setRotation(xssfStyle.getRotation());
            targetStyle.setIndention(xssfStyle.getIndention());
            targetStyle.setShrinkToFit(xssfStyle.getShrinkToFit());
            targetStyle.setLocked(xssfStyle.getLocked());
            targetStyle.setHidden(xssfStyle.getHidden());

            targetStyle.setBorderTop(xssfStyle.getBorderTop());
            targetStyle.setBorderBottom(xssfStyle.getBorderBottom());
            targetStyle.setBorderLeft(xssfStyle.getBorderLeft());
            targetStyle.setBorderRight(xssfStyle.getBorderRight());
            targetStyle.setTopBorderColor(resolveColor(xssfStyle.getTopBorderXSSFColor()));
            targetStyle.setBottomBorderColor(resolveColor(xssfStyle.getBottomBorderXSSFColor()));
            targetStyle.setLeftBorderColor(resolveColor(xssfStyle.getLeftBorderXSSFColor()));
            targetStyle.setRightBorderColor(resolveColor(xssfStyle.getRightBorderXSSFColor()));

            targetStyle.setFillPattern(xssfStyle.getFillPattern());
            targetStyle.setFillForegroundColor(resolveColor(xssfStyle.getFillForegroundXSSFColor()));
            targetStyle.setFillBackgroundColor(resolveColor(xssfStyle.getFillBackgroundXSSFColor()));

            targetStyle.setFont(copyFont(xssfStyle.getFont()));
            return targetStyle;
        }

        /**
         * 字体按特征缓存，相同特征的样式复用同一个 HSSFFont。
         */
        private HSSFFont copyFont(XSSFFont sourceFont) {
            if (sourceFont == null) {
                return targetWorkbook.getFontAt(0);
            }
            short colorIndex = resolveColor(sourceFont.getXSSFColor());
            String fontKey = sourceFont.getFontName() + '|' + sourceFont.getFontHeightInPoints() + '|'
                    + sourceFont.getBold() + '|' + sourceFont.getItalic() + '|' + sourceFont.getStrikeout() + '|'
                    + sourceFont.getUnderline() + '|' + sourceFont.getTypeOffset() + '|' + colorIndex;
            HSSFFont cachedFont = fontCache.get(fontKey);
            if (cachedFont != null) {
                return cachedFont;
            }
            HSSFFont targetFont = targetWorkbook.createFont();
            targetFont.setFontName(sourceFont.getFontName());
            targetFont.setFontHeightInPoints(sourceFont.getFontHeightInPoints());
            targetFont.setBold(sourceFont.getBold());
            targetFont.setItalic(sourceFont.getItalic());
            targetFont.setStrikeout(sourceFont.getStrikeout());
            targetFont.setUnderline(sourceFont.getUnderline());
            targetFont.setTypeOffset(sourceFont.getTypeOffset());
            targetFont.setColor(colorIndex);
            fontCache.put(fontKey, targetFont);
            return targetFont;
        }

        /**
         * 把 XSSFColor 解析为 xls 调色板索引。
         *
         * <p>xlsx 大量使用主题色（{@code theme="8" tint="0.8"}），XSSFColor#getRGB 对这类颜色返回 null，
         * 需要回退到工作簿主题表取基础色，再按 tint 向白/黑方向做明度调整；解析不出来时返回自动色。</p>
         */
        private short resolveColor(XSSFColor color) {
            if (color == null || color.isAuto()) {
                return AUTOMATIC_COLOR_INDEX;
            }
            byte[] rgb = color.getRGB();
            if ((rgb == null || rgb.length < 3) && color.isThemed() && themesTable != null) {
                XSSFColor themeColor = themesTable.getThemeColor(color.getTheme());
                if (themeColor != null) {
                    rgb = themeColor.getRGB();
                }
            }
            if (rgb == null || rgb.length < 3) {
                return color.isIndexed() ? color.getIndex() : AUTOMATIC_COLOR_INDEX;
            }
            int offset = rgb.length >= 4 ? rgb.length - 3 : 0;
            int red = rgb[offset] & 0xFF;
            int green = rgb[offset + 1] & 0xFF;
            int blue = rgb[offset + 2] & 0xFF;
            double tint = color.hasTint() ? color.getTint() : 0D;
            if (tint != 0D) {
                red = applyTint(red, tint);
                green = applyTint(green, tint);
                blue = applyTint(blue, tint);
            }
            return toPaletteIndex(red, green, blue);
        }

        /**
         * Excel 的 tint 语义：正数向白色靠拢，负数向黑色靠拢（RGB 空间近似即可满足配色需求）。
         */
        private int applyTint(int channel, double tint) {
            double result = tint > 0 ? channel + (255 - channel) * tint : channel * (1 + tint);
            return (int) Math.max(0, Math.min(255, Math.round(result)));
        }

        /**
         * 取调色板索引：先找完全一致的颜色，没有则申请一个自定义色位，色位用尽时退化为最接近色。
         */
        private short toPaletteIndex(int red, int green, int blue) {
            byte redByte = (byte) red;
            byte greenByte = (byte) green;
            byte blueByte = (byte) blue;
            HSSFColor exactColor = palette.findColor(redByte, greenByte, blueByte);
            if (exactColor != null) {
                return exactColor.getIndex();
            }
            try {
                HSSFColor addedColor = palette.addColor(redByte, greenByte, blueByte);
                if (addedColor != null) {
                    return addedColor.getIndex();
                }
            } catch (RuntimeException ignored) {
                // 自定义色位已用尽（xls 仅 56 个自定义色），退化为最接近的调色板颜色
            }
            HSSFColor similarColor = palette.findSimilarColor(redByte, greenByte, blueByte);
            return similarColor == null ? AUTOMATIC_COLOR_INDEX : similarColor.getIndex();
        }
    }
}
