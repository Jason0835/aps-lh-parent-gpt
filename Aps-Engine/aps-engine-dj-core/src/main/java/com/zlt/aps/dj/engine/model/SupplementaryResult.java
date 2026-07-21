package com.zlt.aps.dj.engine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 补量结果：机台补量后排产后的班次状态
 */
@Getter
@AllArgsConstructor
public class SupplementaryResult {
    /** 补量后排班次剩余产能（米） */
    private BigDecimal shiftRemainingCapacity;
    /** 补量后排班次剩余台车数 */
    private BigDecimal shiftRemainingTrolleys;
}
