package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.cx.api.domain.entity.MidNightShiftFinish;
import lombok.Data;
import lombok.ToString;

/**
 * 成型工序中夜班完成量汇总对象
 */
@Data
@ToString
public class CxMiddleNightFinishQty extends MidNightShiftFinish {
    private static final long serialVersionUID = 1L;

    /**
     * 中夜班完成量
     */
    private Integer middleNightQty;

    /**
     * 入参 汇总排程日期 yyyyMMdd
     */
    private String scheduleDateStr;
}
