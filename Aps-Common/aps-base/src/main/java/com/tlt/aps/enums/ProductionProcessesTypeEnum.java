package com.tlt.aps.enums;

import lombok.Getter;

/**
 * 工序类型
 * 01-月计划,02-硫化,03-成型,04-胎面,05-胎侧,06--内衬,07-垫胶,08-胎圈,09-钢丝圈,10-斜裁,11-直裁,12-压延,15-零度,16-密炼
 *
 * @author ZLT
 * 20251212
 */
@Getter
public enum ProductionProcessesTypeEnum {
    /**
     * 01 月计划工序
     */
    MONTH_PLAN("01", "月计划工序");

    private String procCode;

    private String desc;

    ProductionProcessesTypeEnum(String procCode, String desc) {
        this.procCode = procCode;
        this.desc = desc;
    }
}
