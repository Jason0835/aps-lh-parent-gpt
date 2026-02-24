package com.tlt.aps.baseVo;


import lombok.Data;

/**
 * 合并单元格定位VO
 */
@Data
public class ExcelCellRangeAddress {

    public ExcelCellRangeAddress(int firstRow, int lastRow, int firstColumn, int lastColumn) {
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.firstColumn = firstColumn;
        this.lastColumn = lastColumn;

    }

    /**
     * 起始行
     */
    private int firstRow;
    /**
     * 结束行
     */
    private int lastRow;
    /**
     * 起始列
     */
    private int firstColumn;
    /**
     * 结束列
     */
    private int lastColumn;

    /**
     * 合并数据所在填充行的列开始位置（用于判断是否需要是否是合并再合并的情况）
     */
//    private int dateColumn;

}
