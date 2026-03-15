package com.zlt.aps.baseVo;

import lombok.Data;

import java.util.List;

/**
 * 下拉列表配置项
 */
@Data
public  class ExcelDropdownVo {
    /**
     * 起始行（从0开始）
     */
    private int firstRow;
    /**
     * 结束行（从0开始）
     */
    private int lastRow;
    /**
     * 起始列（从0开始）
     */
    private int firstCol;
    /**
     * 结束列（从0开始）
     */
    private int lastCol;
    /**
     * 下拉选项列表
     */
    private List<String> options;

    public ExcelDropdownVo(int firstRow, int lastRow, int firstCol, int lastCol, List<String> options) {
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.firstCol = firstCol;
        this.lastCol = lastCol;
        this.options = options;
    }
}
