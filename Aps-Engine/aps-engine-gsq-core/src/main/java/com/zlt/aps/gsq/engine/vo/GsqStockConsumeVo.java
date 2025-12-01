package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

@Data
public class GsqStockConsumeVo {

    /**
     * 钢丝圈代码
     */
    private String steelRingCode;

    /**
     * 半制品对应成型(8点-16点)计划量的消耗量
     */
    private Double cxClass3PlanConsume;

    /**
     * 半制品对应成型(8点-12点)完成量的消耗量
     */
    @Deprecated
    private Double cxFinishConsume;

    /**
     * 昨日夜班半制品生产量
     */
    private Double consume;
}
