package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
/** TM/TC 损耗率规则公共运行态模型。 */
@Data
public class ScheduleLossRuleModel {
    /** 工艺物料编码，TM 对应胎面编码，TC 对应胎侧编码。 */
    protected String processCode;

    /** 工厂编码。 */
    protected String factoryCode;

    /** 机台编码。 */
    protected String machineCode;

    /** 损耗率，百分比。 */
    protected BigDecimal lossRate;

    /** 规则优先级。 */
    protected Integer priority;
}
