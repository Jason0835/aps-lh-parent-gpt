package com.zlt.aps.baseVo;

import com.zlt.aps.baseVo.excelVo.BorderStyleVo;
import lombok.Data;

import java.util.List;

/**
 * Excel样式vo
 */
@Data
public class ExcelStyleVo {

    /**
     * 是否粗体
     */
    private boolean bold =false;

    /**
     * 是否有边框
     */
    private Boolean border = true;

    private Short fontSize;

    /**
     * 字体名称
     */
    private String fontName;
    /**
     *合并单元格位置
     * @return
     */
    private List<ExcelCellRangeAddress> rangeAddresses;

    /**
     * 边框
     */
    private BorderStyleVo borderStyleVo;

    private Short color;


    public boolean getBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }


}
