package com.zlt.aps.cx.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 班次计划量查询结果
 *
 * @author APS Team
 */
@Data
@AllArgsConstructor
public class ShiftPlanResult {
    private int planQty;
    private String shiftName;
}
