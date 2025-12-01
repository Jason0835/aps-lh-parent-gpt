package com.zlt.aps.gdyy.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 纤维压延原线规格值对象 T_GDYY_ORIGINAL_LINE_SPEC
 * 
 * @date 2025-04-14
 */
@Data
public class GdyyOriginalLineSpecVo {
    /** 原线规格 */
    private String originalLineCode;

    /** 原线长度 */
    private BigDecimal originalLineLength;

    /** 备注 */
    private String remark;
}
