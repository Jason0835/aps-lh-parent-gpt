package com.tlt.aps.baseVo.excelVo;

import lombok.Data;
import org.apache.poi.ss.usermodel.BorderStyle;

/**
 * 边框方法
 */
@Data
public class BorderStyleVo {

    public BorderStyleVo() {

    }
    public BorderStyleVo(BorderStyle borderTop, BorderStyle borderBottom, BorderStyle borderLeft, BorderStyle borderRight) {
        this.borderTop = borderTop;
        this.borderBottom = borderBottom;
        this.borderLeft = borderLeft;
        this.borderRight = borderRight;
    }

    private BorderStyle borderTop;

    private BorderStyle borderBottom;

    private BorderStyle borderLeft;

    private BorderStyle borderRight;



}
