package com.zlt.aps.common.core.domain;

import lombok.Data;

@Data
public class CellStyle {

    public CellStyle(int startRowNum, int endRowNum, int startCellNum, int endCellNum, String color, boolean withBorder) {
        this.startRowNum = startRowNum;
        this.endRowNum = endRowNum;
        this.startCellNum = startCellNum;
        this.endCellNum = endCellNum;
        this.color = color;
        this.withBorder = withBorder;
    }

    public CellStyle(int startRowNum, int endRowNum, int startCellNum, int endCellNum, String color, boolean withBorder, boolean bold, String fontName) {
        this.startRowNum = startRowNum;
        this.endRowNum = endRowNum;
        this.startCellNum = startCellNum;
        this.endCellNum = endCellNum;
        this.color = color;
        this.withBorder = withBorder;
        this.bold = bold;
        this.fontName = fontName;
    }

    /**
     * 开始行
     */
    private int startRowNum;
    /**
     * 结束行
     */
    private int endRowNum;
    /**
     * 开始列
     */
    private int startCellNum;
    /**
     * 结束列
     */
    private int endCellNum;

    /**
     * 填充颜色为指定的color
     */
    private String color;
    /**
     * 是否保留边框
     */
    private Boolean withBorder;

    /**
     * 是否加粗
     */
    private Boolean bold;

    /**
     * 字体名称
     */
    private String fontName;

}
