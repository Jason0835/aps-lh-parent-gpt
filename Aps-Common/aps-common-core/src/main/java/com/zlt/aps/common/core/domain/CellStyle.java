package com.zlt.aps.common.core.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class CellStyle {

    /**
     * 无参构造方法：ExcelUtils 中 cellStyleList 是用 fastjson 从 JSON 反序列化得到的，
     * fastjson 要求有无参构造，否则会改用"参数最多的构造方法"作为 creator（只回填构造参数），
     * 导致非构造参数（如 dataFormat）无法回填而变成 null
     */
    public CellStyle() {
    }

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

    public CellStyle(int startRowNum, int endRowNum, int startCellNum, int endCellNum, String color, boolean withBorder, boolean bold, String fontName, String fontColor) {
        this.startRowNum = startRowNum;
        this.endRowNum = endRowNum;
        this.startCellNum = startCellNum;
        this.endCellNum = endCellNum;
        this.color = color;
        this.withBorder = withBorder;
        this.bold = bold;
        this.fontName = fontName;
        this.fontColor = fontColor;
    }

    /**
     * 开始行
     */
    @EqualsAndHashCode.Exclude
    private int startRowNum;
    /**
     * 结束行
     */
    @EqualsAndHashCode.Exclude
    private int endRowNum;
    /**
     * 开始列
     */
    @EqualsAndHashCode.Exclude
    private int startCellNum;
    /**
     * 结束列
     */
    @EqualsAndHashCode.Exclude
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

    /**
     * 字体颜色，设置后字体将以该颜色显示
     */
    private String fontColor;

    /**
     * 自定义单元格数字格式，例如 0;-0;;@（正数正常显示、负数带负号、0值显示空白、文本原样显示）
     */
    private String dataFormat;

}
