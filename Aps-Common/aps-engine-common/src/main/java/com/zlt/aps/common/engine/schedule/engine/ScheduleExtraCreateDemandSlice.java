package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 已按来源版本和成型偏移解析的真实消费段；数量为米，不在门禁中再次换算。
 */
@Data
public class ScheduleExtraCreateDemandSlice {
    /**
     * 成型逻辑班序，必须与当前映射起点使用同一来源日期轴。
     */
    private Integer formingLogicalShiftOrder;
    /**
     * 该来源消费段的米数，不能混入已加损耗的生产量。
     */
    private BigDecimal demandQty;
    /**
     * 工厂时区解析后的真实消费开始时间，epoch毫秒。
     */
    private Long consumptionStartTime;
    /**
     * 该段最晚消费时点；最大保管开启时必须提供，不能将班初当整段消费结束。
     */
    private Long consumptionEndTime;
    /**
     * 是否来自真实成型需求；CLASS8之外的外推不得标成真实记录。
     */
    private Boolean realDemand;
    /**
     * 是否来自当前批次已经冻结的参数化库存覆盖窗口。
     */
    private Boolean approvedDemand;
    /**
     * 是否为CLASS8之后按既有末三班平均规则生成的外推需求。
     */
    private Boolean extrapolatedDemand;
    /**
     * 该消费日期/班位明确开放，未知不能作为开放。
     */
    private Boolean calendarOpen;
}
