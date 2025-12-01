package com.zlt.aps.tm.engine.vo;

import lombok.Data;

@Data
public class TmStockConsumeVo {

    /**
     * 胎面代码
     */
    private String treadCode;

    /**
     * 半制品对应成型(8点-16点)计划量的消耗量
     */
    @Deprecated
    private Double cxClass3PlanConsume;

    /**
     * 半制品对应成型(8点-12点)完成量的消耗量
     */
    @Deprecated
    private Double cxFinishConsume;

    /**
     * 成型一班计划量对应半制品计划消耗量
     */
    private Double cxClass1PlanConsume;

    /**
     * 昨日夜班半制品生产量
     */
    private Double consume;
}
