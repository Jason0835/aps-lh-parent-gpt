package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧班次时间窗口。
 *
 * <p>用于在排程上下文中保存数据加载阶段读取到的班次配置，供任务链计算预计开始时间和预计结束时间。
 * 该对象只承载运行态班次配置，不直接访问数据库。</p>
 */
@Data
public class TcShiftTimeWindow {

    /** 班次顺序 */
    private Integer shiftOrder;

    /** 班次编码 */
    private String shiftCode;

    /** 计划开始时间，格式 HH:mm:ss */
    private String planStartTime;

    /** 计划结束时间，格式 HH:mm:ss */
    private String planEndTime;

    /** 是否跨天，字典 biz_yes_no */
    private String crossDayFlag;

    /** 班次时长，单位小时 */
    private BigDecimal shiftHours;
}
