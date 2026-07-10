package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 插单预演产生的任务级跨班顺延影响。
 */
@Data
@Builder
public class Cd90InsertCarryoverImpact {

    /** 受影响帘布代码。 */
    private String clothCode;
    /** INSERT表示插单帘布，EXISTING表示原后缀帘布。 */
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
