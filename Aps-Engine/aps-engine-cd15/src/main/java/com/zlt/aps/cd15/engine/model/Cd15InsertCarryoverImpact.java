package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 插单预演产生的任务级跨班顺延影响。
 */
@Data
@Builder
public class Cd15InsertCarryoverImpact {

    /** 受影响钢带代码。 */
    private String steelStripCode;
    /** INSERT表示插单钢带，EXISTING表示原后缀钢带。 */
    private String affectedType;
    /** 本次未完整容纳的来源班次。 */
    private String sourceClassField;
    /** 顺延目标班次；超过窗口时为空。 */
    private String targetClassField;
    /** 本次顺延数量。 */
    private BigDecimal carryoverQty;
    /** 本班未完整容纳的真实限制原因。 */
    private String reasonCode;
}
