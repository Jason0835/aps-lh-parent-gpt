package com.zlt.aps.enums;

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
    MONTH_PLAN("01", "月计划工序"),
    /**
     * 02-硫化工序
     */
    LH("02", "硫化工序"),
    /**
     * 03-成型工序
     */
    CX("03", "成型工序"),
    /**
     * 04-胎面工序
     */
    TM("04", "胎面工序"),
    /**
     * 05-胎侧工序
     */
    TC("05", "胎侧工序"),
    /**
     * 06--内衬工序
     */
    NC("06", "内衬工序"),
    /**
     * 07-垫胶工序
     */
    DJ("07", "垫胶工序"),
    /**
     * 08-胎圈工序
     */
    TQ("08", "胎圈工序"),
    /**
     * 09-钢丝圈工序
     */
    GSQ("09", "钢丝圈工序"),
    /**
     * 10-斜裁工序
     */
    CD15("10", "斜裁工序"),
    /**
     * 11-直裁工序
     */
    CD90("11", "直裁工序"),
    /**
     * 12-压延工序
     */
    GDYY("12", "直裁工序"),
    /**
     * 15-零度工序
     */
    LD("15", "零度工序"),
    /**
     * 16-密炼工序
     */
    ML("16", "直裁工序");

    private String procCode;

    private String desc;

    ProductionProcessesTypeEnum(String procCode, String desc) {
        this.procCode = procCode;
        this.desc = desc;
    }
}
