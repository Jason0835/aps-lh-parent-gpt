package com.zlt.aps.lh.util;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.vo.LhMouldChangePlanVo;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * 换模计划模板专用读取器，供单独导入和日计划内的换模Sheet共用。
 */
public final class LhMouldChangePlanImportReader {

    /**
     * 模板前四行为表头、标题及双语列名。
     */
    private static final int HEADER_ROW_COUNT = 4;

    private LhMouldChangePlanImportReader() {
    }

    /**
     * 读取模板明细，在内存中规范Excel时间单元格并保留备注原文。
     *
     * @param fileBytes 原始Excel文件内容
     * @param sheetName 换模计划工作表名称
     * @return 与模板明细顺序一致的导入视图
     * @throws Exception 工作簿解析或通用Excel读取失败时向调用方传递异常
     */
    public static List<LhMouldChangePlanVo> readRows(byte[] fileBytes, String sheetName) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes));
             ByteArrayOutputStream normalizedBytes = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null || sheet.getRow(0) == null) {
                throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
            }
            int endTypeColumn = findColumn(sheet.getRow(0),
                    "ui.data.column.lhMouldChangePlan.changeType");
            int remarkColumn = findColumn(sheet.getRow(0),
                    "ui.data.column.lhMouldChangePlan.remark");
            List<String> remarks = new ArrayList<>();
            IntStream.rangeClosed(HEADER_ROW_COUNT, sheet.getLastRowNum())
                    .mapToObj(sheet::getRow)
                    .filter(LhMouldChangePlanImportReader::hasData)
                    .forEach(row -> {
                        normalizeTimeCell(endTypeColumn < 0 ? null : row.getCell(endTypeColumn));
                        Cell remarkCell = remarkColumn < 0 ? null : row.getCell(remarkColumn);
                        // 通用读取器会trim字符串，此处保留备注首尾空格及多行内容。
                        remarks.add(remarkCell == null ? "" : new DataFormatter().formatCellValue(remarkCell));
                    });
            workbook.write(normalizedBytes);
            List<LhMouldChangePlanVo> rows = new ExcelUtil<>(LhMouldChangePlanVo.class).importExcel(
                    sheetName, new ByteArrayInputStream(normalizedBytes.toByteArray()),
                    0, HEADER_ROW_COUNT, -1);
            IntStream.range(0, rows.size()).forEach(index -> rows.get(index).setRemark(remarks.get(index)));
            return rows;
        }
    }

    /**
     * 按现有国际化表头寻找列，避免依赖固定列序号。
     *
     * @param headerRow 隐藏的模板字段表头
     * @param titleKey  列标题国际化键
     * @return 列号，列不存在时返回-1
     */
    private static int findColumn(Row headerRow, String titleKey) {
        String title = I18nUtil.getMessage(titleKey);
        DataFormatter formatter = new DataFormatter();
        return StreamSupport.stream(headerRow.spliterator(), false)
                .filter(cell -> title.equals(formatter.formatCellValue(cell)))
                .mapToInt(Cell::getColumnIndex).findFirst().orElse(-1);
    }

    /**
     * 判断明细行是否包含单元格，与通用读取器跳过空行的条件保持一致。
     *
     * @param row 模板明细行
     * @return 行内存在非空类型单元格时返回true
     */
    private static boolean hasData(Row row) {
        return row != null && StreamSupport.stream(row.spliterator(), false)
                .anyMatch(cell -> cell.getCellType() != CellType.BLANK);
    }

    /**
     * 将Excel原生时间转为时分文本；保留非零秒以便业务层拒绝非固定时间。
     *
     * @param cell 下机列单元格，不存在时不处理
     */
    private static void normalizeTimeCell(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC
                || !org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
            return;
        }
        double numericValue = cell.getNumericCellValue();
        if (numericValue < 0 || numericValue >= 1) {
            return;
        }
        String timeText = DateUtil.format(cell.getDateCellValue(), "H:mm:ss");
        cell.setCellValue(timeText.endsWith(":00") ? timeText.substring(0, timeText.length() - 3) : timeText);
    }
}
