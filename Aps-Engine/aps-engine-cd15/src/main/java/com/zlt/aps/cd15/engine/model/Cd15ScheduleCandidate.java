package com.zlt.aps.cd15.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当前斜裁班次的待排规格候选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cd15ScheduleCandidate {

    /** 钢带代码。 */
    private String steelStripCode;
    /** 施工材料稳定键。 */
    private String materialKey;
    /** 裁断角度。 */
    private String cuttingAngle;
    /** 斜裁有效宽度，单位毫米。 */
    private BigDecimal craftWidth;
    /** 单片长度，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 大卷幅宽，单位毫米。 */
    private BigDecimal cordWidth;
    /** 标准卷曲长度，单位米。 */
    private BigDecimal curlLength;


    /** 裁断模式：SINGLE或SPLIT。 */
    private String cutMode;
    /** 已有分裁组合稳定键。 */
    private String splitGroupKey;
    /** 大卷代码，对应施工ARTICLE_CROWN_SPEC。 */
    private String bigRollCode;
    /** 当前斜裁班次是否会发生缺料。 */
    private boolean shortageInCurrentShift;
    /** 是否续作规格：6点至本班开始前累计成型消耗 > 0，表示前序班次已为该规格排过产。 */
    private boolean continueFromPreviousShift;
    /** 是否命中新增规格提前生产。 */
    private boolean newSpecAdvance;
    /** 新增规格提前需求是否已换算为斜裁走料米数。 */
    private boolean newSpecAdvanceQuantityNormalized;
    /** 新增规格判定和提前生产原因。 */
    private String newSpecAdvanceAnalysis;
    /** 本次续作或历史生产的原机台；原机台仍可排时优先保持不换机。 */
    private String sourceMachineCode;
    /** 最早缺料时点。 */
    private LocalDateTime earliestShortageTime;
    /** 库存供应成型时长。 */
    private BigDecimal stockSupplyHours;
    /** 定时滚动任务稳定身份键。 */
    private String rollingTaskKey;
    /** 本条原任务实际分配到的待排数量。 */
    private BigDecimal rollingRequestedQuantity;
    /** 本次班初机尾续作可使用的最大工装数；为空表示不限制公平份额。 */
    private Integer maxToolingVehicleCount;
    /** 当前需求是否为已经完成损耗、起排量和取整处理的精确跨班余量。 */
    private boolean exactContinuationQuantity;
    /** 本次是否应用了班初机尾工装公平份额。 */
    private boolean toolingFairShareApplied;
}
