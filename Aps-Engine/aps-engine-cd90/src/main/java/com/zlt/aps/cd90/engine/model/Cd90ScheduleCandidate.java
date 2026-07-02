package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当前直裁班次的待排规格候选项。
 */
@Data
@Builder
public class Cd90ScheduleCandidate {

    /** 帘布代码。 */
    private String clothCode;
    /** 大卷代码，对应施工CORD_SPEC。 */
    private String bigRollCode;
    /** 当前直裁班次是否会发生缺料。 */
    private boolean shortageInCurrentShift;
    /** 是否续作规格：6点至本班开始前累计成型消耗 > 0，表示前序班次已为该规格排过产。 */
    private boolean continueFromPreviousShift;
    /** 是否命中新增规格提前生产。 */
    private boolean newSpecAdvance;
    /** 新增规格提前需求是否已换算为直裁走料米数。 */
    private boolean newSpecAdvanceQuantityNormalized;
    /** 新增规格判定和提前生产原因。 */
    private String newSpecAdvanceAnalysis;
    /** 本次续作或历史生产的原机台；原机台仍可排时优先保持不换机。 */
    private String sourceMachineCode;
    /** 最早缺料时点。 */
    private LocalDateTime earliestShortageTime;
    /** 库存供应成型时长。 */
    private BigDecimal stockSupplyHours;
}
