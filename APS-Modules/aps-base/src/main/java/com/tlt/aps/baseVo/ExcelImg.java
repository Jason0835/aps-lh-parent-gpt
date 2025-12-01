package com.tlt.aps.baseVo;

import lombok.Data;

import java.net.URL;

/**
 * Excel 图片类型
 */
@Data
public class ExcelImg {

    /**
     * 图片url
     */
    private URL imgUrl;
    /**
     * 图片开始行
     */
    private int startRowNum;
    /**
     * 图片结束行
     */
    private int endRowNum;
    /**
     * 图片开始列
     */
    private int startCellNum;
    /**
     * 图片结束列
     */
    private int endCellNum;

    /**
     * 图片类型
     * Workbook.PICTURE_TYPE_JPEG
     * Workbook.PICTURE_TYPE_PNG
     * 不填默认为PICTURE_TYPE_PNG
     */
    private int imgType = 6;

}
