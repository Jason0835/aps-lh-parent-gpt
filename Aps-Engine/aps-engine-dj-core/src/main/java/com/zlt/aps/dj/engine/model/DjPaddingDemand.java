package com.zlt.aps.dj.engine.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 垫胶需求中间模型
 * 承载步骤3/4的输出数据，用于步骤5排产计算
 */
@Data
public class DjPaddingDemand {

    /** 垫胶代码 */
    private String paddingCode;

    /** 垫胶物料名 */
    private String paddingName;

    /** 剩余待排产量（每班由 checkDemandForShift 动态计算），单位：米 */
    private BigDecimal remainingDemand;

    /** 本班接班库存（米） */
    private BigDecimal incomingInventory;

    /** 最早成型需求时间 */
    private Date firstDemandTime;

    /** 单耗（insideCraft） */
    private BigDecimal unitConsume;

    /** 施工号 */
    private String constructionCode;

    /** 外胎规格描述 */
    private String specDesc;

    /** 成型生产状态：0-未生产；1-生产中；2-已收尾 */
    private String productionStatus;

    /** 分配的机台编码 */
    private String machineCode;

    /** 胶料代码 */
    private String glueCode;

    /** 口型板编码 */
    private String mouthPlateCode;

    /** 胶料序号（从 DjGlueOrder 获取） */
    private Integer glueSeq;

    /** 胶料组别编码 */
    private String glueGroupCode;

    /** 台车容量（米/台车），即 curlLength */
    private BigDecimal trolleyCapacity;

    /** 是否已收尾（productionStatus = "2" 时为 true） */
    private boolean isTailFinished;

    /** 是否新规格（库存表中无该垫胶代码记录时为 true） */
    private boolean isNewSpec;

    /** 本班是否需要排产（算法过程中动态标记） */
    private boolean needProduce;

    /** 当前接班库存可覆盖的成型生产班次数（由 checkDemandForShift 计算） */
    private int coverableShiftCount;

    /** 是否供应缺口填补模式：多规格接班库存不足以支撑本班消耗时，仅补本班消耗缺口 */
    private boolean supplyGapMode;

    /** 供应窗口（当前班之后）是否有成型需求，false 表示后续窗口无需求，若本班不生产则无补救机会 */
    private boolean windowHasDemand;
}
