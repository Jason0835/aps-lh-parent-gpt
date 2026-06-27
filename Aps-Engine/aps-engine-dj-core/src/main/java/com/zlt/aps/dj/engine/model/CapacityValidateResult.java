package com.zlt.aps.dj.engine.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产能校验结果
 *
 * @author zlt
 */
@Data
@Accessors(chain = true)
public class CapacityValidateResult {
    private boolean passed;
    private String errorMsg;
    /** 机台定额 */
    private BigDecimal quota;
    /** 当前班次已有计划量总和 */
    private BigDecimal currentTotal;
    /** 新总量（当前+插单） */
    private BigDecimal newTotal;
    /** 超出产能量 */
    private BigDecimal overflowQty;
    /** 受影响的规格列表 */
    private List<String> overflowSpecs;
}
