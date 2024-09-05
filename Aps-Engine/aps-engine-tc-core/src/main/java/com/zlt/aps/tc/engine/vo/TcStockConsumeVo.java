package com.zlt.aps.tc.engine.vo;

import lombok.Data;

@Data
public class TcStockConsumeVo {

    /**
     * 胎侧代码
     */
    private String sidewallCode;

    /**
     * 半制品对应成型(8点-16点)计划量的消耗量
     */
    private Double cxClass3PlanConsume;

    /**
     * 半制品对应成型(8点-12点)完成量的消耗量
     */
    private Double cxFinishConsume;
}
