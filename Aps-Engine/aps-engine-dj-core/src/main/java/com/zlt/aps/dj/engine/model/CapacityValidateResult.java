package com.zlt.aps.dj.engine.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产能校验结果
 * <p>
 * 三档产能判断：
 * <ul>
 *   <li><b>withinQuota=true</b>：插单后该班次计划总量&lt;=机台定额，无产能问题，可直接执行</li>
 *   <li><b>withinQuota=false, passed=true</b>：插单后总量超出定额但未超出实际剩余产能（机台定额-已生产量），
 *       需提示用户会导致延后规格，但用户确认后可执行</li>
 *   <li><b>passed=false</b>：超出实际剩余产能，拒绝插单</li>
 * </ul>
 * </p>
 *
 * @author zlt
 */
@Data
@Accessors(chain = true)
public class CapacityValidateResult {
    private boolean passed;
    /** 是否在机台定额内（newTotal <= quota），true=无溢出可直接执行 */
    private boolean withinQuota;
    private String errorMsg;
    /** 机台定额 */
    private BigDecimal quota;
    /** 当前班次已有计划量总和 */
    private BigDecimal currentTotal;
    /** 新总量（当前+插单） */
    private BigDecimal newTotal;
    /** 当前班次已生产量（完成量） */
    private BigDecimal finishQty;
    /** 实际剩余产能（机台定额 - 已生产量） */
    private BigDecimal remainingCapacity;
    /** 超出产能量 */
    private BigDecimal overflowQty;
    /** 受影响的规格列表 */
    private List<String> overflowSpecs;
}
