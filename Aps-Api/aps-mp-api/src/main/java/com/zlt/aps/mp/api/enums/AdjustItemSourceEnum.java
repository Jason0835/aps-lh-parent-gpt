package com.zlt.aps.mp.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 周程滚动调整明细来源枚举
 * @author wengpc
 */
@AllArgsConstructor
@Getter
public enum AdjustItemSourceEnum {

    SALE_POOL("01", "销售订单池"),
    TRIAL("02","试制量试"),
    MONTH_PLAN("03","月度生产计划"),
    ;

    private String code;
    private String name;

}
